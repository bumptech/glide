package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.GlideOption.OVERRIDE_EXTEND;
import static com.bumptech.glide.annotation.GlideOption.OVERRIDE_NONE;

import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.google.common.base.Strings;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * Generates a new implementation of {@link com.bumptech.glide.request.BaseRequestOptions}
 * containing static versions of methods included in the base class and static and instance versions
 * of all methods annotated with {@link GlideOption} in classes annotated with
 * {@link GlideExtension}.
 *
 * <p>The generated class looks something like this:
 * <pre>
 * <code>
 * public final class GlideOptions
 *     extends com.bumptech.glide.request.BaseRequestOptions<com.bumptech.glide.GlideOptions> {
 *   public static com.bumptech.glide.GlideOptions sizeMultiplierOf(float sizeMultiplier) {
 *     return new com.bumptech.glide.GlideOptions().sizeMultiplier(sizeMultiplier);
 *   }
 *
 *   public static com.bumptech.glide.GlideOptions useUnlimitedSourceGeneratorsPoolOf(
 *       boolean flag) {
 *     return new com.bumptech.glide.GlideOptions().useUnlimitedSourceGeneratorsPool(flag);
 *   }
 *
 *   ... // The rest of the static versions of methods from BaseRequestOptions go here.
 *
 *   // Now on to methods generated from an extension:
 *   public com.bumptech.glide.GlideOptions dontAnimate() {
 *     com.bumptech.glide.integration.gifdecoder.GifOptions.dontAnimate(this);
 *     return this;
 *   }
 *
 *   public static com.bumptech.glide.GlideOptions noAnimate() {
 *     return new com.bumptech.glide.GlideOptions().dontAnimate();
 *   }
 * }
 * </code>
 * </pre>
 * </p>
 */
final class RequestOptionsGenerator {
  private static final String GENERATED_REQUEST_OPTIONS_SIMPLE_NAME = "GlideOptions";
  private static final String BASE_REQUEST_OPTIONS_PACKAGE_NAME = "com.bumptech.glide.request";
  private static final String BASE_REQUEST_OPTIONS_SIMPLE_NAME = "BaseRequestOptions";
  static final String BASE_REQUEST_OPTIONS_QUALIFIED_NAME =
      BASE_REQUEST_OPTIONS_PACKAGE_NAME + "." + BASE_REQUEST_OPTIONS_SIMPLE_NAME;

  static final String REQUEST_OPTIONS_PACKAGE_NAME = "com.bumptech.glide.request";

  private final ProcessingEnvironment processingEnvironment;
  private final ClassName baseRequestOptionsName;
  private final TypeElement baseRequestOptionsType;
  private final ProcessorUtil processorUtil;
  private ClassName glideOptionsName;
  private int nextStaticFieldUniqueId;

  RequestOptionsGenerator(
      ProcessingEnvironment processingEnvironment, ProcessorUtil processorUtil) {
    this.processingEnvironment = processingEnvironment;
    this.processorUtil = processorUtil;

    baseRequestOptionsName = ClassName.get(BASE_REQUEST_OPTIONS_PACKAGE_NAME,
        BASE_REQUEST_OPTIONS_SIMPLE_NAME);

    baseRequestOptionsType = processingEnvironment.getElementUtils().getTypeElement(
        BASE_REQUEST_OPTIONS_QUALIFIED_NAME);
  }

  TypeSpec generate(String generatedCodePackageName, Set<String> glideExtensionClassNames) {
    glideOptionsName =
        ClassName.get(generatedCodePackageName, GENERATED_REQUEST_OPTIONS_SIMPLE_NAME);

    ParameterizedTypeName baseRequestOptionsOfGlideOptions = ParameterizedTypeName
        .get(baseRequestOptionsName, glideOptionsName);

    List<MethodAndStaticVar> staticEquivalents = generateStaticEquivalentsForBaseRequestOptions();
    List<MethodAndStaticVar> methodsForExtensions =
        generateMethodsForExtensions(glideExtensionClassNames);
    List<MethodAndStaticVar> allMethodsAndStaticVars
        = new ArrayList<>(staticEquivalents);
    allMethodsAndStaticVars.addAll(methodsForExtensions);

    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(GENERATED_REQUEST_OPTIONS_SIMPLE_NAME)
        .addAnnotation(
            AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "deprecation")
                .build())
        .addJavadoc(generateClassJavadoc(glideExtensionClassNames))
        .addModifiers(Modifier.FINAL)
        .addModifiers(Modifier.PUBLIC)
        .superclass(baseRequestOptionsOfGlideOptions);

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
    Builder builder = CodeBlock.builder()
        .add("Automatically generated from {@link $T} annotated classes.\n",
            GlideExtension.class)
        .add("\n")
        .add("@see $T\n", baseRequestOptionsName);

    for (String glideExtensionClass : glideExtensionClassNames) {
      builder.add("@see $T\n", ClassName.bestGuess(glideExtensionClass));
    }
    return builder.build();
  }

  private List<MethodAndStaticVar> generateMethodsForExtensions(
      Set<String> glideExtensionClassNames) {
    List<ExecutableElement> requestOptionExtensionMethods =
        processorUtil.findAnnotatedElementsInClasses(
            glideExtensionClassNames, GlideOption.class);

    List<MethodAndStaticVar> result = new ArrayList<>(requestOptionExtensionMethods.size());
    for (ExecutableElement requestOptionsExtensionMethod : requestOptionExtensionMethods) {
      result.addAll(generateMethodForRequestOptionsExtension(requestOptionsExtensionMethod));
    }

    return result;
  }


  private List<MethodAndStaticVar> generateMethodForRequestOptionsExtension(
      ExecutableElement element) {
    boolean isOverridingBaseRequestOptionsMethod = isMethodInBaseRequestOptions(element);
    int overrideType = getOverrideType(element);
    if (isOverridingBaseRequestOptionsMethod && overrideType == OVERRIDE_NONE) {
      throw new IllegalArgumentException("Accidentally attempting to override a method in"
          + " BaseRequestOptions. Add an 'override' value in the @GlideOption annotation"
          + " if this is intentional. Offending method: "
          + element.getEnclosingElement() + "#" + element);
    } else if (!isOverridingBaseRequestOptionsMethod && overrideType != OVERRIDE_NONE) {
      throw new IllegalArgumentException("Requested to override an existing method in"
          + " BaseRequestOptions, but no such method was found. Offending method: "
          + element.getEnclosingElement() + "#" + element);
    }
    String methodName = element.getSimpleName().toString();
    MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
        .addModifiers(Modifier.PUBLIC)
        .addJavadoc(processorUtil.generateSeeMethodJavadoc(element))
        .returns(glideOptionsName);

    // The 0th element is expected to be a BaseRequestOptions object.
    List<? extends VariableElement> parameters =
        element.getParameters().subList(1, element.getParameters().size());

    // Add the correct super() call.
    if (overrideType == OVERRIDE_EXTEND) {
      String callSuper = "super.$L(";
      List<Object> args = new ArrayList<>();
      args.add(element.getSimpleName().toString());
      if (!parameters.isEmpty()) {
        for (VariableElement variable : parameters) {
          callSuper += "$L, ";
          args.add(variable.getSimpleName().toString());
        }
        callSuper = callSuper.substring(0, callSuper.length() - 2);
      }
      callSuper += ")";

      builder.addStatement(callSuper, args.toArray(new Object[0]))
          .addJavadoc(processorUtil.generateSeeMethodJavadoc(
              baseRequestOptionsName, methodName, parameters))
          .addAnnotation(Override.class);
    }

    for (VariableElement variable : parameters) {
      builder.addParameter(getParameterSpec(variable));
    }

    // Adds: <AnnotatedClass>.<thisMethodName>(BaseRequestOptions<?>, <arg1>, <arg2>, <argN>);
    List<Object> args = new ArrayList<>();
    String code = "$T.$L($L, ";
    args.add(ClassName.get(element.getEnclosingElement().asType()));
    args.add(element.getSimpleName().toString());
    args.add("this");
    if (!parameters.isEmpty()) {
      for (VariableElement variable : parameters) {
        code += "$L, ";
        args.add(variable.getSimpleName().toString());
      }
    }
    code = code.substring(0, code.length() - 2);
    code += ")";
    builder.addStatement(code, args.toArray(new Object[0]));

    builder.addStatement("return this");

    List<MethodAndStaticVar> result = new ArrayList<>();
    result.add(new MethodAndStaticVar(builder.build()));
    if (!isOverridingBaseRequestOptionsMethod) {
      result.add(generateStaticMethodEquivalent(element, true /*ignoreFirst*/));
    }

    return result;
  }

  private List<MethodAndStaticVar> generateStaticEquivalentsForBaseRequestOptions() {
    List<ExecutableElement> instanceMethodsThatReturnBaseRequestOptions =
        processorUtil.findInstanceMethodsReturning(baseRequestOptionsType, baseRequestOptionsType);
    List<MethodAndStaticVar> staticMethods = new ArrayList<>();
    for (ExecutableElement element : instanceMethodsThatReturnBaseRequestOptions) {
      staticMethods.add(generateStaticMethodEquivalent(element, false /*ignoreFirst*/));
    }
    return staticMethods;
  }

  private MethodAndStaticVar generateStaticMethodEquivalent(ExecutableElement instanceMethod,
      boolean ignoreFirst) {
    boolean skipStaticMethod = skipStaticMethod(instanceMethod);
    if (skipStaticMethod) {
      return new MethodAndStaticVar();
    }
    String staticMethodName = getStaticMethodName(instanceMethod);
    String instanceMethodName = instanceMethod.getSimpleName().toString();
    if (Strings.isNullOrEmpty(staticMethodName)) {
      if (instanceMethodName.startsWith("dont")) {
        staticMethodName = "no" + instanceMethodName.replace("dont", "");
      } else {
        staticMethodName = instanceMethodName + "Of";
      }
    }
    boolean memoize = memoizeStaticMethod(instanceMethod);

    MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder(staticMethodName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addJavadoc(processorUtil.generateSeeMethodJavadoc(instanceMethod))
        .returns(glideOptionsName);

    List<? extends VariableElement> parameters = instanceMethod.getParameters();
    if (ignoreFirst) {
      if (parameters.isEmpty()) {
        throw new IllegalArgumentException(
            "Expected non-empty parameters for: " + instanceMethod);
      }
      // Remove is not supported.
      parameters = parameters.subList(1, parameters.size());
    }

    String createNewOptionAndCall = "new $T().$L(";
    if (!parameters.isEmpty()) {
      for (VariableElement parameter : parameters) {
        methodSpecBuilder.addParameter(getParameterSpec(parameter));
        createNewOptionAndCall += parameter.getSimpleName().toString();
        // use the Application Context to avoid memory leaks.
        if (memoize && isAndroidContext(parameter)) {
          createNewOptionAndCall += ".getApplicationContext()";
        }
        createNewOptionAndCall += ", ";
      }
      createNewOptionAndCall =
          createNewOptionAndCall.substring(0, createNewOptionAndCall.length() - 2);
    }
    createNewOptionAndCall += ")";

    FieldSpec requiredStaticField = null;
    if (memoize) {
      // if (GlideOptions.<methodName> == null) {
      //   GlideOptions.<methodName> = new GlideOptions().<methodName>().autoLock()
      // }

      // Mix in an incrementing unique id to handle method overloading.
      String staticVariableName = staticMethodName + nextStaticFieldUniqueId++;
      requiredStaticField = FieldSpec.builder(glideOptionsName, staticVariableName)
          .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
          .build();
      methodSpecBuilder.beginControlFlow(
          "if ($T.$N == null)", glideOptionsName, staticVariableName)
          .addStatement("$T.$N =\n" + createNewOptionAndCall + ".$N",
              glideOptionsName, staticVariableName, glideOptionsName, instanceMethodName,
              "autoClone()")
          .endControlFlow()
          .addStatement("return $T.$N", glideOptionsName, staticVariableName);
    } else {
      // return new GlideOptions().<methodName>()
      methodSpecBuilder.addStatement(
          "return " + createNewOptionAndCall, glideOptionsName, instanceMethodName);
    }

    List<? extends TypeParameterElement> typeParameters = instanceMethod.getTypeParameters();
    for (TypeParameterElement typeParameterElement : typeParameters) {
      methodSpecBuilder.addTypeVariable(
          TypeVariableName.get(typeParameterElement.getSimpleName().toString()));
    }

    return new MethodAndStaticVar(methodSpecBuilder.build(), requiredStaticField);
  }

  private boolean isAndroidContext(VariableElement variableElement) {
    Element element = processingEnvironment.getTypeUtils().asElement(variableElement.asType());
    return element.toString().equals("android.content.Context");
  }

  private boolean isMethodInBaseRequestOptions(ExecutableElement toFind) {
    // toFind is a method in a GlideExtension whose first argument is a BaseRequestOptions<?> type.
    // Since we're comparing against methods in BaseRequestOptions itself, we need to drop that
    // first type.
    List<String> toFindParameterNames = getComparableParameterNames(toFind, true /*skipFirst*/);
    String toFindSimpleName = toFind.getSimpleName().toString();
    for (Element element : baseRequestOptionsType.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD) {
        continue;
      }
      ExecutableElement inBase = (ExecutableElement) element;
      if (toFindSimpleName.equals(inBase.getSimpleName().toString())) {
        List<String> parameterNamesInBase =
            getComparableParameterNames(inBase, false /*skipFirst*/);
        if (parameterNamesInBase.equals(toFindParameterNames)) {
          return true;
        }
      }
    }
    return false;
  }

  private static ParameterSpec getParameterSpec(VariableElement variable) {
    return ParameterSpec.builder(
        TypeName.get(variable.asType()), variable.getSimpleName().toString()).build();
  }

  private static List<String> getComparableParameterNames(
      ExecutableElement element, boolean skipFirst) {
    List<? extends VariableElement> parameters = element.getParameters();
    if (skipFirst) {
      parameters = parameters.subList(1, parameters.size());
    }
    List<String> result = new ArrayList<>(parameters.size());
    for (VariableElement parameter : parameters) {
      result.add(parameter.asType().toString());
    }
    return result;
  }

  private static int getOverrideType(ExecutableElement element) {
    GlideOption glideOption =
        element.getAnnotation(GlideOption.class);
    return glideOption.override();
  }

  @Nullable
  private static String getStaticMethodName(ExecutableElement element) {
    GlideOption glideOption =
        element.getAnnotation(GlideOption.class);
    String result = glideOption != null ? glideOption.staticMethodName() : null;
    return Strings.emptyToNull(result);
  }

  private static boolean memoizeStaticMethod(ExecutableElement element) {
    GlideOption glideOption =
        element.getAnnotation(GlideOption.class);
    return glideOption != null && glideOption.memoizeStaticMethod();
  }

  private static boolean skipStaticMethod(ExecutableElement element) {
    GlideOption glideOption =
        element.getAnnotation(GlideOption.class);
    return glideOption != null && glideOption.skipStaticMethod();
  }

  private static final class MethodAndStaticVar {
    @Nullable
    final MethodSpec method;
    @Nullable
    final FieldSpec staticField;

    MethodAndStaticVar() {
      this(null /*method*/);
    }

    MethodAndStaticVar(@Nullable MethodSpec method) {
      this(method, null /*staticField*/);
    }

    MethodAndStaticVar(@Nullable MethodSpec method, @Nullable FieldSpec staticField) {
      this.method = method;
      this.staticField = staticField;
    }
  }
}
