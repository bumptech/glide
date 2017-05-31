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
 * Generates a new implementation of a AppGlideModule that calls all included LibraryGlideModules
 * and the original AppGlideModule.
 *
 * <p>The generated class will always call the AppGlideModule last to give it priority over choices
 * made or classes registered in LibraryGlideModules.
 *
 * <p>Android logging is included to allow developers to see exactly which modules are included at
 * runtime.
 *
 * <p>The generated class looks something like this:
 * <pre>
 * <code>
 *  final class GeneratedAppGlideModuleImpl extends com.bumptech.glide.GeneratedAppGlideModule {
 *    private final com.bumptech.glide.samples.giphy.GiphyGlideModule appGlideModule;
 *
 *    GeneratedAppGlideModule() {
 *      appGlideModule = new com.bumptech.glide.samples.giphy.GiphyGlideModule();
 *      if (android.util.Log.isLoggable("Glide", android.util.Log.DEBUG)) {
 *        android.util.Log.d("Glide", "Discovered AppGlideModule from annotation:"
 *            + " com.bumptech.glide.samples.giphy.GiphyGlideModule");
 *        android.util.Log.d("Glide", "Discovered LibraryGlideModule from annotation:"
 *            + "com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule");
 *      }
 *    }
 *
 *    {@literal @java.lang.Override}
 *    public void applyOptions(android.content.Context context,
 *        com.bumptech.glide.GlideBuilder builder) {
 *      appGlideModule.applyOptions(context, builder);
 *    }
 *
 *    {@literal @java.lang.Override}
 *    public void registerComponents(android.content.Context context,
 *        com.bumptech.glide.Registry registry) {
 *      new com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule()
 *          .registerComponents(context, registry);
 *      appGlideModule.registerComponents(context, registry);
 *    }
 *
 *    {@literal @java.lang.Override}
 *    public boolean isManifestParsingEnabled() {
 *      return appGlideModule.isManifestParsingEnabled();
 *    }
 *
 *    {@literal @java.lang.Override}
 *    public java.util.Set<java.lang.Class<?>> getExcludedModuleClasses() {
 *      return appGlideModule.getExcludedModuleClasses();
 *    }
 *  }
 * </code>
 * </pre>
 */
final class AppModuleGenerator {
  static final String GENERATED_ROOT_MODULE_PACKAGE_NAME = "com.bumptech.glide";
  private static final String GLIDE_LOG_TAG = "Glide";
  private static final String GENERATED_APP_MODULE_IMPL_SIMPLE_NAME =
      "GeneratedAppGlideModuleImpl";
  private static final String GENERATED_ROOT_MODULE_SIMPLE_NAME = "GeneratedAppGlideModule";
  private final ProcessorUtil processorUtil;

  AppModuleGenerator(ProcessorUtil processorUtil) {
    this.processorUtil = processorUtil;
  }

  TypeSpec generate(TypeElement appGlideModule, Set<String> libraryGlideModuleClassNames) {
    ClassName appGlideModuleClassName = ClassName.get(appGlideModule);
    Set<String> excludedGlideModuleClassNames =
        getExcludedGlideModuleClassNames(appGlideModule);

    MethodSpec constructor =
        generateConstructor(
            appGlideModuleClassName, libraryGlideModuleClassNames, excludedGlideModuleClassNames);

    MethodSpec registerComponents =
        generateRegisterComponents(libraryGlideModuleClassNames, excludedGlideModuleClassNames);

    MethodSpec getExcludedModuleClasses =
        generateGetExcludedModuleClasses(excludedGlideModuleClassNames);

    MethodSpec applyOptions =
        MethodSpec.methodBuilder("applyOptions")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(ClassName.get("android.content", "Context"), "context")
            .addParameter(ClassName.get("com.bumptech.glide", "GlideBuilder"), "builder")
            .addStatement("appGlideModule.applyOptions(context, builder)", appGlideModule)
            .build();

    MethodSpec isManifestParsingEnabled =
        MethodSpec.methodBuilder("isManifestParsingEnabled")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(boolean.class)
            .addStatement("return appGlideModule.isManifestParsingEnabled()", appGlideModule)
            .build();

    Builder builder = TypeSpec.classBuilder(GENERATED_APP_MODULE_IMPL_SIMPLE_NAME)
        .addModifiers(Modifier.FINAL)
        .addAnnotation(
            AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "deprecation")
                .build()
        )
        .superclass(
            ClassName.get(GENERATED_ROOT_MODULE_PACKAGE_NAME, GENERATED_ROOT_MODULE_SIMPLE_NAME))
        .addField(appGlideModuleClassName, "appGlideModule", Modifier.PRIVATE, Modifier.FINAL)
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

  private MethodSpec generateRegisterComponents(Set<String> libraryGlideModuleClassNames,
      Set<String> excludedGlideModuleClassNames) {
    MethodSpec.Builder registerComponents =
        MethodSpec.methodBuilder("registerComponents")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(ClassName.get("android.content", "Context"), "context")
            .addParameter(ClassName.get("com.bumptech.glide", "Registry"), "registry");

    for (String glideModule : libraryGlideModuleClassNames) {
      if (excludedGlideModuleClassNames.contains(glideModule)) {
        continue;
      }
      ClassName moduleClassName = ClassName.bestGuess(glideModule);
      registerComponents.addStatement(
          "new $T().registerComponents(context, registry)", moduleClassName);
    }
    // Order matters here. The AppGlideModule must be called last.
    registerComponents.addStatement("appGlideModule.registerComponents(context, registry)");
    return registerComponents.build();
  }

  private MethodSpec generateConstructor(ClassName appGlideModule,
      Set<String> libraryGlideModuleClassNames, Set<String> excludedGlideModuleClassNames) {
    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
    constructorBuilder.addStatement("appGlideModule = new $T()", appGlideModule);

    ClassName androidLogName = ClassName.get("android.util", "Log");

    // Add some log lines to indicate to developers which modules where discovered.
    constructorBuilder.beginControlFlow("if ($T.isLoggable($S, $T.DEBUG))",
        androidLogName, GLIDE_LOG_TAG, androidLogName);
    constructorBuilder.addStatement("$T.d($S, $S)", androidLogName, GLIDE_LOG_TAG,
        "Discovered AppGlideModule from annotation: " + appGlideModule);
    // Excluded GlideModule classes from the manifest are logged in Glide's singleton.
    for (String glideModule : libraryGlideModuleClassNames) {
      ClassName moduleClassName = ClassName.bestGuess(glideModule);
      if (excludedGlideModuleClassNames.contains(glideModule)) {
        constructorBuilder.addStatement("$T.d($S, $S)", androidLogName, GLIDE_LOG_TAG,
            "AppGlideModule excludes LibraryGlideModule from annotation: " + moduleClassName);
      } else {
        constructorBuilder.addStatement("$T.d($S, $S)", androidLogName, GLIDE_LOG_TAG,
            "Discovered LibraryGlideModule from annotation: " + moduleClassName);
      }
    }
    constructorBuilder.endControlFlow();
    return constructorBuilder.build();
  }

  private Set<String> getExcludedGlideModuleClassNames(TypeElement appGlideModule) {
    return processorUtil.findClassValuesFromAnnotationOnClassAsNames(
        appGlideModule, Excludes.class);
  }
}
