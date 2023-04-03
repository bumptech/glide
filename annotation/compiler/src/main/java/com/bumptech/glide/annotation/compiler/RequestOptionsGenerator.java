package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * Generates a new implementation of {@code com.bumptech.glide.request.RequestOptions} containing
 * static versions of methods included in the base class and static and instance versions of all
 * methods annotated with {@link GlideOption} in classes annotated with {@link GlideExtension}.
 *
 * <p>The generated class looks something like this:
 *
 * <pre>
 * <code>
 * public final class GlideOptions extends com.bumptech.glide.request.RequestOptions {
 *
 *   public static com.google.android.apps.photos.glide.GlideOptions signatureOf(
 *       com.bumptech.glide.load.Key arg0) {
 *     return new com.google.android.apps.photos.glide.GlideOptions()
 *         .apply(com.bumptech.glide.request.RequestOptions.signatureOf(arg0));
 *   }
 *
 *   ... // The rest of the static versions of methods from RequestOptions go here.
 *
 *   // Now on to methods generated from an extension:
 *   public com.bumptech.glide.GlideOptions dontAnimate() {
 *     com.bumptech.glide.integration.gifdecoder.GifOptions.dontAnimate(this);
 *     return this;
 *   }
 *
 *   public static com.bumptech.glide.GlideOptions noAnimation() {
 *     return new com.bumptech.glide.GlideOptions().dontAnimate();
 *   }
 * }
 * </code>
 * </pre>
 */
final class RequestOptionsGenerator {
  private static final String GENERATED_REQUEST_OPTIONS_SIMPLE_NAME = "GlideOptions";
  static final String REQUEST_OPTIONS_PACKAGE_NAME = "com.bumptech.glide.request";
  private static final String REQUEST_OPTIONS_SIMPLE_NAME = "RequestOptions";
  static final String REQUEST_OPTIONS_QUALIFIED_NAME =
      REQUEST_OPTIONS_PACKAGE_NAME + "." + REQUEST_OPTIONS_SIMPLE_NAME;

  static final String BASE_REQUEST_OPTIONS_SIMPLE_NAME = "BaseRequestOptions";
  static final String BASE_REQUEST_OPTIONS_QUALIFIED_NAME =
      REQUEST_OPTIONS_PACKAGE_NAME + "." + BASE_REQUEST_OPTIONS_SIMPLE_NAME;

  private int nextFieldId;

  private final ClassName requestOptionsName;
  private final TypeElement requestOptionsType;
  private final ProcessorUtil processorUtil;
  private final RequestOptionsOverrideGenerator requestOptionsOverrideGenerator;

  private ClassName glideOptionsName;

  RequestOptionsGenerator(
      ProcessingEnvironment processingEnvironment, ProcessorUtil processorUtil) {
    this.processorUtil = processorUtil;

    requestOptionsName = ClassName.get(REQUEST_OPTIONS_PACKAGE_NAME, REQUEST_OPTIONS_SIMPLE_NAME);

    requestOptionsType =
        processingEnvironment.getElementUtils().getTypeElement(REQUEST_OPTIONS_QUALIFIED_NAME);

    requestOptionsOverrideGenerator =
        new RequestOptionsOverrideGenerator(processingEnvironment, processorUtil);
  }

  TypeSpec generate(String generatedCodePackageName, Set<String> glideExtensionClassNames) {
    glideOptionsName =
        ClassName.get(generatedCodePackageName, GENERATED_REQUEST_OPTIONS_SIMPLE_NAME);

    RequestOptionsExtensionGenerator requestOptionsExtensionGenerator =
        new RequestOptionsExtensionGenerator(glideOptionsName, processorUtil);
    List<MethodAndStaticVar> instanceMethodsForExtensions =
        FluentIterable.from(
                requestOptionsExtensionGenerator.generateInstanceMethodsForExtensions(
                    glideExtensionClassNames))
            .transform(
                new Function<MethodSpec, MethodAndStaticVar>() {
                  @Override
                  public MethodAndStaticVar apply(MethodSpec input) {
                    return new MethodAndStaticVar(input);
                  }
                })
            .toList();

    List<MethodAndStaticVar> staticMethodsForExtensions =
        FluentIterable.from(
                requestOptionsExtensionGenerator.getRequestOptionExtensionMethods(
                    glideExtensionClassNames))
            .filter(
                new Predicate<ExecutableElement>() {
                  @Override
                  public boolean apply(ExecutableElement input) {
                    return !skipStaticMethod(input);
                  }
                })
            .transform(
                new Function<ExecutableElement, MethodAndStaticVar>() {
                  @Override
                  public MethodAndStaticVar apply(ExecutableElement input) {
                    return generateStaticMethodEquivalentForExtensionMethod(input);
                  }
                })
            .toList();

    List<MethodAndStaticVar> methodsForExtensions = new ArrayList<>();
    methodsForExtensions.addAll(instanceMethodsForExtensions);
    methodsForExtensions.addAll(staticMethodsForExtensions);

    Set<MethodSignature> extensionMethodSignatures =
        ImmutableSet.copyOf(
            Iterables.transform(
                methodsForExtensions,
                new Function<MethodAndStaticVar, MethodSignature>() {
                  @Override
                  public MethodSignature apply(MethodAndStaticVar f) {
                    return new MethodSignature(f.method);
                  }
                }));

    List<MethodAndStaticVar> staticOverrides = generateStaticMethodOverridesForRequestOptions();
    List<MethodSpec> instanceOverrides =
        requestOptionsOverrideGenerator.generateInstanceMethodOverridesForRequestOptions(
            glideOptionsName);

    List<MethodAndStaticVar> allMethodsAndStaticVars = new ArrayList<>();
    for (MethodAndStaticVar item : staticOverrides) {
      if (extensionMethodSignatures.contains(new MethodSignature(item.method))) {
        continue;
      }
      allMethodsAndStaticVars.add(item);
    }
    for (MethodSpec methodSpec : instanceOverrides) {
      if (extensionMethodSignatures.contains(new MethodSignature(methodSpec))) {
        continue;
      }
      allMethodsAndStaticVars.add(new MethodAndStaticVar(methodSpec));
    }
    allMethodsAndStaticVars.addAll(methodsForExtensions);

    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(GENERATED_REQUEST_OPTIONS_SIMPLE_NAME)
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "deprecation")
                    .build())
            .addJavadoc(generateClassJavadoc(glideExtensionClassNames))
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(Cloneable.class)
            .superclass(requestOptionsName);

    for (MethodAndStaticVar methodAndStaticVar : allMethodsAndStaticVars) {
      if (methodAndStaticVar.method != null) {
        classBuilder.addMethod(methodAndStaticVar.method);
      }
      if (methodAndStaticVar.staticField != null) {
        classBuilder.addField(methodAndStaticVar.staticField);
      }
    }
    return classBuilder.build();
  }

  private CodeBlock generateClassJavadoc(Set<String> glideExtensionClassNames) {
    Builder builder =
        CodeBlock.builder()
            .add(
                "Automatically generated from {@link $T} annotated classes.\n",
                GlideExtension.class)
            .add("\n")
            .add("@see $T\n", requestOptionsName);

    for (String glideExtensionClass : glideExtensionClassNames) {
      builder.add("@see $T\n", ClassName.bestGuess(glideExtensionClass));
    }
    return builder.build();
  }

  private List<MethodAndStaticVar> generateStaticMethodOverridesForRequestOptions() {
    List<ExecutableElement> staticMethodsThatReturnRequestOptions =
        processorUtil.findStaticMethodsReturning(requestOptionsType, requestOptionsType);
    List<MethodAndStaticVar> staticMethods = new ArrayList<>();
    for (ExecutableElement element : staticMethodsThatReturnRequestOptions) {
      if (element.getAnnotation(Deprecated.class) != null) {
        continue;
      }
      staticMethods.add(generateStaticMethodEquivalentForRequestOptionsStaticMethod(element));
    }
    return staticMethods;
  }

  /**
   * This method is a bit of a hack, but it lets us tie the static version of a method with the
   * instance version. In turn that lets us call the instance versions on the generated subclass,
   * instead of just delegating to the RequestOptions static methods. Using the instance methods on
   * the generated subclass allows our static methods to properly call code that overrides an
   * existing method in RequestOptions.
   *
   * <p>The string names here just map between the static methods in {@code
   * com.bumptech.glide.request.RequestOptions} and the instance methods they call.
   */
  private static String getInstanceMethodNameFromStaticMethodName(String staticMethodName) {
    String equivalentInstanceMethodName;
    if ("bitmapTransform".equals(staticMethodName)) {
      equivalentInstanceMethodName = "transform";
    } else if ("decodeTypeOf".equals(staticMethodName)) {
      equivalentInstanceMethodName = "decode";
    } else if (staticMethodName.endsWith("Transform")) {
      equivalentInstanceMethodName = staticMethodName.substring(0, staticMethodName.length() - 9);
    } else if (staticMethodName.endsWith("Of")) {
      equivalentInstanceMethodName = staticMethodName.substring(0, staticMethodName.length() - 2);
    } else if ("noTransformation".equals(staticMethodName)) {
      equivalentInstanceMethodName = "dontTransform";
    } else if ("noAnimation".equals(staticMethodName)) {
      equivalentInstanceMethodName = "dontAnimate";
    } else if (staticMethodName.equals("option")) {
      equivalentInstanceMethodName = "set";
    } else {
      throw new IllegalArgumentException("Unrecognized static method name: " + staticMethodName);
    }
    return equivalentInstanceMethodName;
  }

  private MethodAndStaticVar generateStaticMethodEquivalentForRequestOptionsStaticMethod(
      ExecutableElement staticMethod) {
    boolean memoize = memoizeStaticMethodFromArguments(staticMethod);
    String staticMethodName = staticMethod.getSimpleName().toString();

    String equivalentInstanceMethodName =
        getInstanceMethodNameFromStaticMethodName(staticMethodName);

    MethodSpec.Builder methodSpecBuilder =
        MethodSpec.methodBuilder(staticMethodName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc(processorUtil.generateSeeMethodJavadoc(staticMethod))
            .returns(glideOptionsName);

    StringBuilder createNewOptionAndCall =
        createNewOptionAndCall(
            memoize, methodSpecBuilder, "new $T().$N(", processorUtil.getParameters(staticMethod));

    FieldSpec requiredStaticField = null;
    if (memoize) {
      // Generates code that looks like:
      // if (GlideOptions.<methodName> == null) {
      //   GlideOptions.<methodName> = new GlideOptions().<methodName>().autoClone()
      // }

      // Mix in an incrementing unique id to handle method overloading.
      String staticVariableName = staticMethodName + nextFieldId++;
      requiredStaticField =
          FieldSpec.builder(glideOptionsName, staticVariableName)
              .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
              .build();
      methodSpecBuilder
          .beginControlFlow("if ($T.$N == null)", glideOptionsName, staticVariableName)
          .addStatement(
              "$T.$N =\n" + createNewOptionAndCall + ".$N",
              glideOptionsName,
              staticVariableName,
              glideOptionsName,
              equivalentInstanceMethodName,
              "autoClone()")
          .endControlFlow()
          .addStatement("return $T.$N", glideOptionsName, staticVariableName);
    } else {
      // Generates code that looks like:
      // return new GlideOptions().<methodName>()
      methodSpecBuilder.addStatement(
          "return " + createNewOptionAndCall, glideOptionsName, equivalentInstanceMethodName);
    }

    List<? extends TypeParameterElement> typeParameters = staticMethod.getTypeParameters();
    for (TypeParameterElement typeParameterElement : typeParameters) {
      methodSpecBuilder.addTypeVariable(
          TypeVariableName.get(typeParameterElement.getSimpleName().toString()));
    }

    methodSpecBuilder
        .addAnnotation(processorUtil.checkResult())
        .addAnnotation(processorUtil.nonNull());

    return new MethodAndStaticVar(methodSpecBuilder.build(), requiredStaticField);
  }

  private static boolean memoizeStaticMethodFromArguments(ExecutableElement staticMethod) {
    return staticMethod.getParameters().isEmpty()
        || (staticMethod.getParameters().size() == 1
            && staticMethod
                .getParameters()
                .get(0)
                .getSimpleName()
                .toString()
                .equals("android.content.Context"));
  }

  private StringBuilder createNewOptionAndCall(
      boolean memoize,
      MethodSpec.Builder methodSpecBuilder,
      String start,
      List<ParameterSpec> specs) {
    StringBuilder createNewOptionAndCall = new StringBuilder(start);
    if (!specs.isEmpty()) {
      methodSpecBuilder.addParameters(specs);
      for (ParameterSpec parameter : specs) {
        createNewOptionAndCall.append(parameter.name);
        // use the Application Context to avoid memory leaks.
        if (memoize && isAndroidContext(parameter)) {
          createNewOptionAndCall.append(".getApplicationContext()");
        }
        createNewOptionAndCall.append(", ");
      }
      createNewOptionAndCall =
          new StringBuilder(
              createNewOptionAndCall.substring(0, createNewOptionAndCall.length() - 2));
    }
    createNewOptionAndCall.append(")");
    return createNewOptionAndCall;
  }

  private boolean isAndroidContext(ParameterSpec parameter) {
    return parameter.type.toString().equals("android.content.Context");
  }

  private MethodAndStaticVar generateStaticMethodEquivalentForExtensionMethod(
      ExecutableElement instanceMethod) {
    String staticMethodName = getStaticMethodName(instanceMethod);
    String instanceMethodName = instanceMethod.getSimpleName().toString();
    if (Strings.isNullOrEmpty(staticMethodName)) {
      if (instanceMethodName.startsWith("dont")) {
        staticMethodName = "no" + instanceMethodName.replace("dont", "");
      } else {
        staticMethodName = instanceMethodName + "Of";
      }
    }
    boolean memoize = memoizeStaticMethodFromAnnotation(instanceMethod);

    //noinspection ResultOfMethodCallIgnored
    Preconditions.checkNotNull(staticMethodName);
    MethodSpec.Builder methodSpecBuilder =
        MethodSpec.methodBuilder(staticMethodName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc(processorUtil.generateSeeMethodJavadoc(instanceMethod))
            .varargs(instanceMethod.isVarArgs())
            .returns(glideOptionsName);

    List<? extends VariableElement> parameters = instanceMethod.getParameters();

    // Always remove the first parameter because it's always RequestOptions in extensions. The
    // actual method we want to generate will pass the RequestOptions in to the extension method,
    // but should not itself require a RequestOptions object to be passed in.
    if (parameters.isEmpty()) {
      throw new IllegalArgumentException("Expected non-empty parameters for: " + instanceMethod);
    }
    // Remove is not supported.
    parameters = parameters.subList(1, parameters.size());

    StringBuilder createNewOptionAndCall =
        createNewOptionAndCall(
            memoize, methodSpecBuilder, "new $T().$L(", processorUtil.getParameters(parameters));

    FieldSpec requiredStaticField = null;
    if (memoize) {
      // Generates code that looks like:
      // if (GlideOptions.<methodName> == null) {
      //   GlideOptions.<methodName> = new GlideOptions().<methodName>().autoClone()
      // }

      // Mix in an incrementing unique id to handle method overloading.
      String staticVariableName = staticMethodName + nextFieldId++;
      requiredStaticField =
          FieldSpec.builder(glideOptionsName, staticVariableName)
              .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
              .build();
      methodSpecBuilder
          .beginControlFlow("if ($T.$N == null)", glideOptionsName, staticVariableName)
          .addStatement(
              "$T.$N =\n" + createNewOptionAndCall + ".$N",
              glideOptionsName,
              staticVariableName,
              glideOptionsName,
              instanceMethodName,
              "autoClone()")
          .endControlFlow()
          .addStatement("return $T.$N", glideOptionsName, staticVariableName);
    } else {
      // Generates code that looks like:
      // return new GlideOptions().<methodName>()
      methodSpecBuilder.addStatement(
          "return " + createNewOptionAndCall, glideOptionsName, instanceMethodName);
    }

    List<? extends TypeParameterElement> typeParameters = instanceMethod.getTypeParameters();
    for (TypeParameterElement typeParameterElement : typeParameters) {
      methodSpecBuilder.addTypeVariable(
          TypeVariableName.get(typeParameterElement.getSimpleName().toString()));
    }

    methodSpecBuilder.addAnnotation(processorUtil.checkResult());

    return new MethodAndStaticVar(methodSpecBuilder.build(), requiredStaticField);
  }

  @Nullable
  private static String getStaticMethodName(ExecutableElement element) {
    GlideOption glideOption = element.getAnnotation(GlideOption.class);
    String result = glideOption != null ? glideOption.staticMethodName() : null;
    return Strings.emptyToNull(result);
  }

  private static boolean memoizeStaticMethodFromAnnotation(ExecutableElement element) {
    GlideOption glideOption = element.getAnnotation(GlideOption.class);
    return glideOption != null && glideOption.memoizeStaticMethod();
  }

  private static boolean skipStaticMethod(ExecutableElement element) {
    GlideOption glideOption = element.getAnnotation(GlideOption.class);
    return glideOption != null && glideOption.skipStaticMethod();
  }

  private static final class MethodAndStaticVar {
    @Nullable final MethodSpec method;
    @Nullable final FieldSpec staticField;

    MethodAndStaticVar(@Nullable MethodSpec method) {
      this(method, null /*staticField*/);
    }

    MethodAndStaticVar(@Nullable MethodSpec method, @Nullable FieldSpec staticField) {
      this.method = method;
      this.staticField = staticField;
    }
  }

  private static final class MethodSignature {
    private final TypeName returnType;
    private final List<TypeName> parameterTypes;
    private final boolean isStatic;
    private final String name;

    MethodSignature(MethodSpec spec) {
      name = spec.name;
      isStatic = spec.modifiers.contains(Modifier.STATIC);
      returnType = spec.returnType;
      parameterTypes =
          Lists.transform(
              spec.parameters,
              new Function<ParameterSpec, TypeName>() {
                @Nullable
                @Override
                public TypeName apply(ParameterSpec parameterSpec) {
                  return parameterSpec.type;
                }
              });
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof MethodSignature) {
        MethodSignature other = (MethodSignature) o;
        return name.equals(other.name)
            && returnType.equals(other.returnType)
            && parameterTypes.equals(other.parameterTypes)
            && isStatic == other.isStatic;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name, returnType, parameterTypes, isStatic);
    }
  }
}
