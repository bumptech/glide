package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.Excludes;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import com.squareup.javapoet.WildcardTypeName;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * Generates a new implementation of a RootGlideModule that calls all included ChildGlideModules and
 * the original RootGlideModule.
 *
 * <p>The generated class will always call the RootGlideModule last to give it priority over choices
 * made or classes registered in ChildGlideModules.
 *
 * <p>Android logging is included to allow developers to see exactly which modules are included at
 * runtime.
 *
 * <p>The generated class looks something like this:
 * <pre>
 * <code>
 *  final class GeneratedRootGlideModuleImpl extends com.bumptech.glide.GeneratedRootGlideModule {
 *    private final com.bumptech.glide.samples.giphy.GiphyGlideModule rootGlideModule;
 *
 *    GeneratedRootGlideModule() {
 *      rootGlideModule = new com.bumptech.glide.samples.giphy.GiphyGlideModule();
 *      if (android.util.Log.isLoggable("Glide", android.util.Log.DEBUG)) {
 *        android.util.Log.d("Glide", "Discovered RootGlideModule from annotation:"
 *            + " com.bumptech.glide.samples.giphy.GiphyGlideModule");
 *        android.util.Log.d("Glide", "Discovered ChildGlideModule from annotation:"
 *            + "com.bumptech.glide.integration.okhttp3.OkHttpChildGlideModule");
 *      }
 *    }
 *
 *    {@literal @java.lang.Override}
 *    public void applyOptions(android.content.Context context,
 *        com.bumptech.glide.GlideBuilder builder) {
 *      rootGlideModule.applyOptions(context, builder);
 *    }
 *
 *    {@literal @java.lang.Override}
 *    public void registerComponents(android.content.Context context,
 *        com.bumptech.glide.Registry registry) {
 *      new com.bumptech.glide.integration.okhttp3.OkHttpChildGlideModule()
 *          .registerComponents(context, registry);
 *      rootGlideModule.registerComponents(context, registry);
 *    }
 *
 *    {@literal @java.lang.Override}
 *    public boolean isManifestParsingEnabled() {
 *      return rootGlideModule.isManifestParsingEnabled();
 *    }
 *
 *    {@literal @java.lang.Override}
 *    public java.util.Set<java.lang.Class<?>> getExcludedModuleClasses() {
 *      return rootGlideModule.getExcludedModuleClasses();
 *    }
 *  }
 * </code>
 * </pre>
 */
final class RootModuleGenerator {
  static final String GENERATED_ROOT_MODULE_PACKAGE_NAME = "com.bumptech.glide";
  private static final String GLIDE_LOG_TAG = "Glide";
  private static final String GENERATED_ROOT_MODULE_IMPL_SIMPLE_NAME =
      "GeneratedRootGlideModuleImpl";
  private static final String GENERATED_ROOT_MODULE_SIMPLE_NAME = "GeneratedRootGlideModule";
  private final ProcessorUtil processorUtil;

  RootModuleGenerator(ProcessorUtil processorUtil) {
    this.processorUtil = processorUtil;
  }

  TypeSpec generate(TypeElement rootGlideModule, Set<String> childGlideModuleClassNames) {
    ClassName rootGlideModuleClassName = ClassName.get(rootGlideModule);
    Set<String> excludedGlideModuleClassNames =
        getExcludedGlideModuleClassNames(rootGlideModule);

    MethodSpec constructor =
        generateConstructor(
            rootGlideModuleClassName, childGlideModuleClassNames, excludedGlideModuleClassNames);

    MethodSpec registerComponents =
        generateRegisterComponents(childGlideModuleClassNames, excludedGlideModuleClassNames);

    MethodSpec getExcludedModuleClasses =
        generateGetExcludedModuleClasses(excludedGlideModuleClassNames);

    MethodSpec applyOptions =
        MethodSpec.methodBuilder("applyOptions")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(ClassName.get("android.content", "Context"), "context")
            .addParameter(ClassName.get("com.bumptech.glide", "GlideBuilder"), "builder")
            .addStatement("rootGlideModule.applyOptions(context, builder)", rootGlideModule)
            .build();

    MethodSpec isManifestParsingEnabled =
        MethodSpec.methodBuilder("isManifestParsingEnabled")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(boolean.class)
            .addStatement("return rootGlideModule.isManifestParsingEnabled()", rootGlideModule)
            .build();

    Builder builder = TypeSpec.classBuilder(GENERATED_ROOT_MODULE_IMPL_SIMPLE_NAME)
        .addModifiers(Modifier.FINAL)
        .addAnnotation(
            AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "deprecation")
                .build()
        )
        .superclass(
            ClassName.get(GENERATED_ROOT_MODULE_PACKAGE_NAME, GENERATED_ROOT_MODULE_SIMPLE_NAME))
        .addField(rootGlideModuleClassName, "rootGlideModule", Modifier.PRIVATE, Modifier.FINAL)
        .addMethod(constructor)
        .addMethod(applyOptions)
        .addMethod(registerComponents)
        .addMethod(isManifestParsingEnabled)
        .addMethod(getExcludedModuleClasses);

    ClassName generatedRequestManagerFactoryClassName =
        ClassName.get(
            RequestManagerFactoryGenerator.GENERATED_REQUEST_MANAGER_FACTORY_PACKAGE_NAME,
            RequestManagerFactoryGenerator.GENERATED_REQUEST_MANAGER_FACTORY_SIMPLE_NAME);

    builder.addMethod(
        MethodSpec.methodBuilder("getRequestManagerFactory")
            .addAnnotation(Override.class)
            .returns(generatedRequestManagerFactoryClassName)
            .addStatement("return new $T()", generatedRequestManagerFactoryClassName)
            .build());
    return builder.build();
  }

  // TODO: When we drop support for parsing GlideModules from AndroidManifests, remove this method.
  private MethodSpec generateGetExcludedModuleClasses(Set<String> excludedClassNames) {
    TypeName wildCardOfObject = WildcardTypeName.subtypeOf(Object.class);
    ParameterizedTypeName classOfWildcardOfObjet =
        ParameterizedTypeName.get(ClassName.get(Class.class), wildCardOfObject);
    ParameterizedTypeName setOfClassOfWildcardOfObject =
        ParameterizedTypeName.get(ClassName.get(Set.class), classOfWildcardOfObjet);
    ParameterizedTypeName hashSetOfClassOfWildcardOfObject =
        ParameterizedTypeName.get(ClassName.get(HashSet.class), classOfWildcardOfObjet);
    MethodSpec.Builder builder = MethodSpec.methodBuilder("getExcludedModuleClasses")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(setOfClassOfWildcardOfObject);

    if (excludedClassNames.isEmpty()) {
      builder.addStatement("return $T.emptySet()", Collections.class);
    } else {
      builder.addStatement(
          "$T excludedClasses = new $T()", setOfClassOfWildcardOfObject,
          hashSetOfClassOfWildcardOfObject);
      for (String excludedClassName : excludedClassNames) {
        // TODO: Remove this when we no longer support manifest parsing.
        // Using a Literal ($L) instead of a type ($T) to get a fully qualified import that allows
        // us to suppress deprecation warnings. Aimed at deprecated GlideModules.
        builder.addStatement("excludedClasses.add($L.class)", excludedClassName);
      }
      builder.addStatement("return excludedClasses");
    }

    return builder.build();
  }

  private MethodSpec generateRegisterComponents(Set<String> childGlideModuleClassNames,
      Set<String> excludedGlideModuleClassNames) {
    MethodSpec.Builder registerComponents =
        MethodSpec.methodBuilder("registerComponents")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(ClassName.get("android.content", "Context"), "context")
            .addParameter(ClassName.get("com.bumptech.glide", "Registry"), "registry");

    for (String glideModule : childGlideModuleClassNames) {
      if (excludedGlideModuleClassNames.contains(glideModule)) {
        continue;
      }
      ClassName moduleClassName = ClassName.bestGuess(glideModule);
      registerComponents.addStatement(
          "new $T().registerComponents(context, registry)", moduleClassName);
    }
    // Order matters here. The RootGlideModule must be called last.
    registerComponents.addStatement("rootGlideModule.registerComponents(context, registry)");
    return registerComponents.build();
  }

  private MethodSpec generateConstructor(ClassName rootGlideModule,
      Set<String> childGlideModuleClassNames, Set<String> excludedGlideModuleClassNames) {
    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
    constructorBuilder.addStatement("rootGlideModule = new $T()", rootGlideModule);

    ClassName androidLogName = ClassName.get("android.util", "Log");

    // Add some log lines to indicate to developers which modules where discovered.
    constructorBuilder.beginControlFlow("if ($T.isLoggable($S, $T.DEBUG))",
        androidLogName, GLIDE_LOG_TAG, androidLogName);
    constructorBuilder.addStatement("$T.d($S, $S)", androidLogName, GLIDE_LOG_TAG,
        "Discovered RootGlideModule from annotation: " + rootGlideModule);
    // Excluded GlideModule classes from the manifest are logged in Glide's singleton.
    for (String glideModule : childGlideModuleClassNames) {
      ClassName moduleClassName = ClassName.bestGuess(glideModule);
      if (excludedGlideModuleClassNames.contains(glideModule)) {
        constructorBuilder.addStatement("$T.d($S, $S)", androidLogName, GLIDE_LOG_TAG,
            "RootGlideModule excludes ChildGlideModule from annotation: " + moduleClassName);
      } else {
        constructorBuilder.addStatement("$T.d($S, $S)", androidLogName, GLIDE_LOG_TAG,
            "Discovered ChildGlideModule from annotation: " + moduleClassName);
      }
    }
    constructorBuilder.endControlFlow();
    return constructorBuilder.build();
  }

  private Set<String> getExcludedGlideModuleClassNames(TypeElement rootGlideModule) {
    return processorUtil.findClassValuesFromAnnotationOnClassAsNames(
        rootGlideModule, Excludes.class);
  }
}
