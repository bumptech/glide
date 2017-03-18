package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.ExtendsRequestOptions.OVERRIDE_EXTEND;
import static com.bumptech.glide.annotation.ExtendsRequestOptions.OVERRIDE_NONE;

import com.bumptech.glide.annotation.ExtendsRequestOptions;
import com.bumptech.glide.annotation.GlideExtension;
import com.google.common.base.Strings;
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
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Generates a new implementation of {@link com.bumptech.glide.request.BaseRequestOptions}
 * containing static versions of methods included in the base class and static and instance versions
 * of all methods annotated with {@link ExtendsRequestOptions} in classes annotated with
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
  static final String GENERATED_REQUEST_OPTIONS_PACKAGE_NAME = "com.bumptech.glide";
  private static final String BASE_REQUEST_OPTIONS_PACKAGE_NAME = "com.bumptech.glide.request";
  private static final String BASE_REQUEST_OPTIONS_SIMPLE_NAME = "BaseRequestOptions";
  private static final String BASE_REQUEST_OPTIONS_QUALIFIED_NAME =
      BASE_REQUEST_OPTIONS_PACKAGE_NAME + "." + BASE_REQUEST_OPTIONS_SIMPLE_NAME;

  private final ProcessingEnvironment processingEnvironment;
  private final ClassName glideOptionsName;
  private final ParameterizedTypeName baseRequestOptionsOfGlideOptions;
  private final ClassName baseRequestOptionsName;
  private final TypeElement baseRequestOptionsType;
  private int nextStaticFieldUniqueId;

  RequestOptionsGenerator(
      ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;

    glideOptionsName = ClassName.get(GENERATED_REQUEST_OPTIONS_PACKAGE_NAME,
        GENERATED_REQUEST_OPTIONS_SIMPLE_NAME);
    baseRequestOptionsName = ClassName.get(BASE_REQUEST_OPTIONS_PACKAGE_NAME,
        BASE_REQUEST_OPTIONS_SIMPLE_NAME);
    baseRequestOptionsOfGlideOptions =
        ParameterizedTypeName.get(baseRequestOptionsName, glideOptionsName);

    baseRequestOptionsType = processingEnvironment.getElementUtils().getTypeElement(
        BASE_REQUEST_OPTIONS_QUALIFIED_NAME);
  }

  TypeSpec generate(Set<String> glideExtensionClassNames) {
    List<MethodAndStaticVar> staticEquivalents = generateStaticEquivalentsForBaseRequestOptions();
    List<MethodAndStaticVar> methodsForExtensions =
        generateMethodsForExtensions(glideExtensionClassNames);
    List<MethodAndStaticVar> allMethodsAndStaticVars
        = new ArrayList<>(staticEquivalents);
    allMethodsAndStaticVars.addAll(methodsForExtensions);

    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(GENERATED_REQUEST_OPTIONS_SIMPLE_NAME)
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
    List<ExecutableElement> requestOptionExtensionMethods = new ArrayList<>();
    for (String glideExtensionClassName : glideExtensionClassNames) {
      TypeElement glideExtension = processingEnvironment.getElementUtils()
          .getTypeElement(glideExtensionClassName);
      for (Element element : glideExtension.getEnclosedElements()) {
        if (element.getAnnotation(ExtendsRequestOptions.class) != null) {
          requestOptionExtensionMethods.add((ExecutableElement) element);
        }
      }
    }

    List<MethodAndStaticVar> result = new ArrayList<>(requestOptionExtensionMethods.size());
    for (ExecutableElement requestOptionsExtensionMethod : requestOptionExtensionMethods) {
      result.addAll(generateMethodForRequestOptionsExtension(requestOptionsExtensionMethod));
    }

    return result;
  }

  /**
   * Returns a safe String to use in a Javadoc that will function in a link.
   *
   * <p>This method exists because by Javadoc doesn't handle type parameters({@literal <T>}
   * in {@literal BaseRequestOptions<T>} for example).
   */
  private Object getJavadocSafeName(Element element) {
    Types typeUtils = processingEnvironment.getTypeUtils();
    TypeMirror type = element.asType();
    if (typeUtils.asElement(type) == null) {
      // If there is no Element, it's a primitive and can't have additional types, so we're done.
      return ClassName.get(element.asType());
    }
    Name simpleName = typeUtils.asElement(type).getSimpleName();
    return ClassName.bestGuess(simpleName.toString());
  }

  private CodeBlock generateSeeMethodJavadoc(ExecutableElement method) {
    // Use the simple name of the containing type instead of just the containing type's TypeMirror
    // so that we avoid appending <CHILD> or other type arguments to the class and breaking
    // Javadoc's linking.
    // With this we get @see BaseRequestOptions#methodName().
    // With just ClassName.get(element.getEnclosingElement().asType()), we get:
    // @see BaseRequestOptions<CHILD>#methodName().
    return generateSeeMethodJavadoc(getJavadocSafeName(method.getEnclosingElement()),
        method.getSimpleName().toString(), method.getParameters());
  }

  private CodeBlock generateSeeMethodJavadoc(
      Object methodContainingClassName, String methodSimpleName,
      List<? extends VariableElement> methodParameters) {
    String javadocString = "@see $T#$L(";
    List<Object> javadocArgs = new ArrayList<>();
    javadocArgs.add(methodContainingClassName);
    javadocArgs.add(methodSimpleName);

    for (VariableElement variable : methodParameters) {
      javadocString += "$T, ";
      javadocArgs.add(getJavadocSafeName(variable));

    }
    if (javadocArgs.size() > 2) {
      javadocString = javadocString.substring(0, javadocString.length() - 2);
    }
    javadocString += ")\n";
    return CodeBlock.of(javadocString, javadocArgs.toArray(new Object[0]));
  }

  private List<MethodAndStaticVar> generateMethodForRequestOptionsExtension(
      ExecutableElement element) {
    boolean isOverridingBaseRequestOptionsMethod = isMethodInBaseRequestOptions(element);
    int overrideType = getOverrideType(element);
    if (isOverridingBaseRequestOptionsMethod && overrideType == OVERRIDE_NONE) {
      throw new IllegalArgumentException("Accidentally attempting to override a method in"
          + " BaseRequestOptions. Add an 'override' value in the @ExtendsRequestOptions annotation"
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
        .addJavadoc(generateSeeMethodJavadoc(element))
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
          .addJavadoc(generateSeeMethodJavadoc(baseRequestOptionsName, methodName, parameters))
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
    List<ExecutableElement> elements = new ArrayList<>();
    for (Element element : baseRequestOptionsType.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD
        ||  !element.getModifiers().contains(Modifier.PUBLIC)
        ||  element.getModifiers().contains(Modifier.STATIC)) {
        continue;
      }
      ExecutableElement executableElement = (ExecutableElement) element;
      if (returnsBaseRequestOptionsObject(executableElement)) {
        elements.add(executableElement);
      }
    }

    List<MethodAndStaticVar> staticMethods = new ArrayList<>();
    for (ExecutableElement element : elements) {
      staticMethods.add(generateStaticMethodEquivalent(element, false /*ignoreFirst*/));
    }
    return staticMethods;
  }

  private boolean returnsBaseRequestOptionsObject(ExecutableElement method) {
    return processingEnvironment.getTypeUtils().isAssignable(
        method.getReturnType(),
        baseRequestOptionsType.asType());
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
        .addJavadoc(generateSeeMethodJavadoc(instanceMethod))
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
              "autoLock()")
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
    ExtendsRequestOptions extendsRequestOptions =
        element.getAnnotation(ExtendsRequestOptions.class);
    return extendsRequestOptions.override();
  }

  @Nullable
  private static String getStaticMethodName(ExecutableElement element) {
    ExtendsRequestOptions extendsRequestOptions =
        element.getAnnotation(ExtendsRequestOptions.class);
    String result = extendsRequestOptions != null ? extendsRequestOptions.staticMethodName() : null;
    return Strings.emptyToNull(result);
  }

  private static boolean memoizeStaticMethod(ExecutableElement element) {
    ExtendsRequestOptions extendsRequestOptions =
        element.getAnnotation(ExtendsRequestOptions.class);
    return extendsRequestOptions != null && extendsRequestOptions.memoizeStaticMethod();
  }

  private static boolean skipStaticMethod(ExecutableElement element) {
    ExtendsRequestOptions extendsRequestOptions =
        element.getAnnotation(ExtendsRequestOptions.class);
    return extendsRequestOptions != null && extendsRequestOptions.skipStaticMethod();
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
