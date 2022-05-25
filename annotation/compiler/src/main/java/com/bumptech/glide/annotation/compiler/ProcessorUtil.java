package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.GlideAnnotationProcessor.DEBUG;

import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Type.ClassType;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/** Utilities for writing classes and logging. */
final class ProcessorUtil {
  private static final String GLIDE_MODULE_PACKAGE_NAME = "com.bumptech.glide.module";
  private static final String APP_GLIDE_MODULE_SIMPLE_NAME = "AppGlideModule";
  private static final String LIBRARY_GLIDE_MODULE_SIMPLE_NAME = "LibraryGlideModule";
  private static final String APP_GLIDE_MODULE_QUALIFIED_NAME =
      GLIDE_MODULE_PACKAGE_NAME + "." + APP_GLIDE_MODULE_SIMPLE_NAME;
  private static final String LIBRARY_GLIDE_MODULE_QUALIFIED_NAME =
      GLIDE_MODULE_PACKAGE_NAME + "." + LIBRARY_GLIDE_MODULE_SIMPLE_NAME;
  private static final String COMPILER_PACKAGE_NAME =
      GlideAnnotationProcessor.class.getPackage().getName();
  private static final ClassName SUPPORT_NONNULL_ANNOTATION =
      ClassName.get("android.support.annotation", "NonNull");
  private static final ClassName JETBRAINS_NOTNULL_ANNOTATION =
      ClassName.get("org.jetbrains.annotations", "NotNull");
  private static final ClassName ANDROIDX_NONNULL_ANNOTATION =
      ClassName.get("androidx.annotation", "NonNull");
  private static final ClassName SUPPORT_CHECK_RESULT_ANNOTATION =
      ClassName.get("android.support.annotation", "CheckResult");
  private static final ClassName ANDROIDX_CHECK_RESULT_ANNOTATION =
      ClassName.get("androidx.annotation", "CheckResult");
  private static final ClassName SUPPORT_VISIBLE_FOR_TESTING =
      ClassName.get("android.support.annotation", "VisibleForTesting");
  private static final ClassName ANDROIDX_VISIBLE_FOR_TESTING =
      ClassName.get("androidx.annotation", "VisibleForTesting");

  private final ProcessingEnvironment processingEnv;
  private final TypeElement appGlideModuleType;
  private final TypeElement libraryGlideModuleType;
  private int round;

  ProcessorUtil(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;

    appGlideModuleType =
        processingEnv.getElementUtils().getTypeElement(APP_GLIDE_MODULE_QUALIFIED_NAME);
    libraryGlideModuleType =
        processingEnv.getElementUtils().getTypeElement(LIBRARY_GLIDE_MODULE_QUALIFIED_NAME);
  }

  void process() {
    round++;
  }

  boolean isAppGlideModule(TypeElement element) {
    return processingEnv.getTypeUtils().isAssignable(element.asType(), appGlideModuleType.asType());
  }

  boolean isLibraryGlideModule(TypeElement element) {
    return processingEnv
        .getTypeUtils()
        .isAssignable(element.asType(), libraryGlideModuleType.asType());
  }

  boolean isExtension(TypeElement element) {
    return element.getAnnotation(GlideExtension.class) != null;
  }

  int getOverrideType(ExecutableElement element) {
    GlideOption glideOption = element.getAnnotation(GlideOption.class);
    return glideOption.override();
  }

  void writeIndexer(TypeSpec indexer) {
    writeClass(COMPILER_PACKAGE_NAME, indexer);
  }

  void writeClass(String packageName, TypeSpec clazz) {
    try {
      debugLog("Writing class:\n" + clazz);
      JavaFile.builder(packageName, clazz)
          .skipJavaLangImports(true)
          .build()
          .writeTo(processingEnv.getFiler());
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  List<ExecutableElement> findAnnotatedElementsInClasses(
      Set<String> classNames, Class<? extends Annotation> annotationClass) {
    List<ExecutableElement> result = new ArrayList<>();
    for (String glideExtensionClassName : classNames) {
      TypeElement glideExtension =
          processingEnv.getElementUtils().getTypeElement(glideExtensionClassName);
      for (Element element : glideExtension.getEnclosedElements()) {
        if (element.getAnnotation(annotationClass) != null) {
          result.add((ExecutableElement) element);
        }
      }
    }
    return result;
  }

  List<TypeElement> getElementsFor(Class<? extends Annotation> clazz, RoundEnvironment env) {
    Collection<? extends Element> annotatedElements = env.getElementsAnnotatedWith(clazz);
    return ElementFilter.typesIn(annotatedElements);
  }

  /**
   * Generates a Javadoc code block for generated methods that delegate to methods in {@link
   * GlideExtension}s.
   *
   * <p>The generated block looks something like this:
   *
   * <pre>
   * <code>
   *   {@literal @see} com.extension.package.name.ExtensionClassName#extensionMethod(arg1, argN)
   * </code>
   * </pre>
   *
   * @param method The method from the {@link GlideExtension} annotated class that the generated
   *     method this Javadoc will be attached to delegates to.
   */
  CodeBlock generateSeeMethodJavadoc(ExecutableElement method) {
    // Use the simple name of the containing type instead of just the containing type's TypeMirror
    // so that we avoid appending <CHILD> or other type arguments to the class and breaking
    // Javadoc's linking.
    // With this we get @see RequestOptions#methodName().
    // With just ClassName.get(element.getEnclosingElement().asType()), we get:
    // @see RequestOptions<CHILD>#methodName().
    return generateSeeMethodJavadoc(
        getJavadocSafeName(method.getEnclosingElement()),
        method.getSimpleName().toString(),
        method.getParameters());
  }

  /**
   * Generates a Javadoc block for generated methods that delegate to other methods.
   *
   * <p>The generated block looks something like this:
   *
   * <pre>
   * <code>
   *     {@literal @see} com.package.ClassContainingMethod.methodSimpleName(
   *         methodParam1, methodParamN)
   * </code>
   * </pre>
   *
   * @param nameOfClassContainingMethod The simple class name of the class containing the method
   *     without any generic types like {@literal <T>}.
   * @param methodSimpleName The name of the method.
   * @param methodParameters A maybe empty list of all the parameters for the method in question.
   */
  CodeBlock generateSeeMethodJavadoc(
      TypeName nameOfClassContainingMethod,
      String methodSimpleName,
      List<? extends VariableElement> methodParameters) {
    return generateSeeMethodJavadocInternal(
        nameOfClassContainingMethod,
        methodSimpleName,
        Lists.transform(
            methodParameters,
            new Function<VariableElement, Object>() {
              @Override
              public Object apply(VariableElement input) {
                return getJavadocSafeName(input);
              }
            }));
  }

  CodeBlock generateSeeMethodJavadoc(TypeName nameOfClassContainingMethod, MethodSpec methodSpec) {
    return generateSeeMethodJavadocInternal(
        nameOfClassContainingMethod,
        methodSpec.name,
        Lists.transform(
            methodSpec.parameters,
            new Function<ParameterSpec, Object>() {
              @Override
              public Object apply(ParameterSpec input) {
                return input.type;
              }
            }));
  }

  private CodeBlock generateSeeMethodJavadocInternal(
      TypeName nameOfClassContainingMethod, String methodName, List<Object> safeParameterNames) {
    StringBuilder javadocString = new StringBuilder("@see $T#$L(");
    List<Object> javadocArgs = new ArrayList<>();
    javadocArgs.add(nameOfClassContainingMethod);
    javadocArgs.add(methodName);

    for (Object param : safeParameterNames) {
      javadocString.append("$T, ");
      javadocArgs.add(param);
    }
    if (javadocArgs.size() > 2) {
      javadocString = new StringBuilder(javadocString.substring(0, javadocString.length() - 2));
    }
    javadocString.append(")\n");
    return CodeBlock.of(javadocString.toString(), javadocArgs.toArray(new Object[0]));
  }

  /**
   * Returns a safe String to use in a Javadoc that will function in a link.
   *
   * <p>This method exists because by Javadoc doesn't handle type parameters({@literal <T>} in
   * {@literal RequestOptions<T>} for example).
   */
  private TypeName getJavadocSafeName(Element element) {
    Types typeUtils = processingEnv.getTypeUtils();
    TypeMirror type = element.asType();
    if (typeUtils.asElement(type) == null) {
      // If there is no Element, it's a primitive and can't have additional types, so we're done.
      return ClassName.get(element.asType());
    }
    Name simpleName = typeUtils.asElement(type).getSimpleName();
    return ClassName.bestGuess(simpleName.toString());
  }

  void debugLog(String toLog) {
    if (DEBUG) {
      infoLog(toLog);
    }
  }

  void infoLog(String toLog) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "[" + round + "] " + toLog);
  }

  static CodeBlock generateCastingSuperCall(TypeName toReturn, MethodSpec method) {
    return CodeBlock.builder()
        .add("return ($T) super.$N(", toReturn, method.name)
        .add(
            FluentIterable.from(method.parameters)
                .transform(
                    new Function<ParameterSpec, String>() {
                      @Override
                      public String apply(ParameterSpec input) {
                        return input.name;
                      }
                    })
                .join(Joiner.on(",")))
        .add(");\n")
        .build();
  }

  MethodSpec.Builder overriding(ExecutableElement method) {
    String methodName = method.getSimpleName().toString();

    MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName).addAnnotation(Override.class);

    Set<Modifier> modifiers = method.getModifiers();
    modifiers = new LinkedHashSet<>(modifiers);
    modifiers.remove(Modifier.ABSTRACT);
    Modifier defaultModifier = null;
    // Modifier.DEFAULT doesn't exist until Java 8.
    try {
      defaultModifier = Modifier.valueOf("DEFAULT");
    } catch (IllegalArgumentException e) {
      // Ignored.
    }
    modifiers.remove(defaultModifier);

    builder = builder.addModifiers(modifiers);

    for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
      TypeVariable var = (TypeVariable) typeParameterElement.asType();
      builder = builder.addTypeVariable(TypeVariableName.get(var));
    }

    builder =
        builder
            .returns(TypeName.get(method.getReturnType()))
            .addParameters(getParameters(method))
            .varargs(method.isVarArgs());

    for (TypeMirror thrownType : method.getThrownTypes()) {
      builder = builder.addException(TypeName.get(thrownType));
    }

    return builder;
  }

  List<ParameterSpec> getParameters(ExecutableElement method) {
    return getParameters(method.getParameters());
  }

  List<ParameterSpec> getParameters(List<? extends VariableElement> parameters) {
    List<ParameterSpec> result = new ArrayList<>();
    for (VariableElement parameter : parameters) {
      result.add(getParameter(parameter));
    }
    return dedupedParameters(result);
  }

  private static List<ParameterSpec> dedupedParameters(List<ParameterSpec> parameters) {
    boolean hasDupes = false;
    Set<String> names = new HashSet<>();
    for (ParameterSpec parameter : parameters) {
      String name = parameter.name;
      if (names.contains(name)) {
        hasDupes = true;
      } else {
        names.add(name);
      }
    }

    if (hasDupes) {
      List<ParameterSpec> copy = parameters;
      parameters = new ArrayList<>();
      for (int i = 0; i < copy.size(); i++) {
        ParameterSpec parameter = copy.get(i);
        parameters.add(
            ParameterSpec.builder(parameter.type, parameter.name + i)
                .addModifiers(parameter.modifiers)
                .addAnnotations(parameter.annotations)
                .build());
      }
    }

    return parameters;
  }

  private ParameterSpec getParameter(VariableElement parameter) {
    TypeName type = TypeName.get(parameter.asType());
    return ParameterSpec.builder(type, computeParameterName(parameter, type))
        .addModifiers(parameter.getModifiers())
        .addAnnotations(getAnnotations(parameter))
        .build();
  }

  private static String computeParameterName(VariableElement parameter, TypeName type) {
    String rawClassName = type.withoutAnnotations().toString();

    String name;

    if (type.isPrimitive() || type.isBoxedPrimitive()) {
      name = getSmartPrimitiveParameterName(parameter);
    } else {
      if (rawClassName.contains("<") && rawClassName.contains(">")) {
        String[] preGenericSplit = rawClassName.split("<");
        String preGeneric = preGenericSplit[0];
        String[] postGenericSplit = rawClassName.split(">");
        String postGeneric = postGenericSplit[postGenericSplit.length - 1];
        if (postGenericSplit.length > 1) {
          rawClassName = preGeneric + postGeneric;
        } else {
          rawClassName = preGeneric;
        }
      }

      String[] qualifiers = rawClassName.split("\\.");
      rawClassName = qualifiers[qualifiers.length - 1];

      rawClassName = applySmartParameterNameReplacements(rawClassName);

      boolean allCaps = true;
      for (char c : rawClassName.toCharArray()) {
        if (Character.isLowerCase(c)) {
          allCaps = false;
          break;
        }
      }
      if (allCaps) {
        name = rawClassName.toLowerCase();
      } else {
        int indexOfLastWordStart = 0;
        char[] chars = rawClassName.toCharArray();
        for (int i = 0, charArrayLength = chars.length; i < charArrayLength; i++) {
          char c = chars[i];
          if (Character.isUpperCase(c)) {
            indexOfLastWordStart = i;
          }
        }
        rawClassName = rawClassName.substring(indexOfLastWordStart, rawClassName.length());

        name =
            Character.toLowerCase(rawClassName.charAt(0))
                + rawClassName.substring(1, rawClassName.length());
      }
    }

    return name;
  }

  private static String getSmartPrimitiveParameterName(VariableElement parameter) {
    for (AnnotationMirror annotation : parameter.getAnnotationMirrors()) {
      String annotationName = annotation.getAnnotationType().toString().toUpperCase();
      if (annotationName.endsWith("RES")) {
        // Catch annotations like StringRes
        return "id";
      } else if (annotationName.endsWith("RANGE")) {
        // Catch annotations like IntRange
        return "value";
      }
    }

    return parameter.getSimpleName().toString();
  }

  private static String applySmartParameterNameReplacements(String name) {
    name = name.replace("[]", "s");
    name = name.replace(Class.class.getSimpleName(), "clazz");
    name = name.replace(Object.class.getSimpleName(), "o");
    return name;
  }

  private List<AnnotationSpec> getAnnotations(VariableElement element) {
    List<AnnotationSpec> result = new ArrayList<>();
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      result.add(maybeConvertSupportLibraryAnnotation(mirror));
    }
    return result;
  }

  private AnnotationSpec maybeConvertSupportLibraryAnnotation(AnnotationMirror mirror) {
    String annotationName = mirror.getAnnotationType().asElement().toString();
    boolean preferAndroidX = visibleForTesting().equals(ANDROIDX_VISIBLE_FOR_TESTING);
    ImmutableBiMap<ClassName, ClassName> map =
        ImmutableBiMap.<ClassName, ClassName>builder()
            .put(SUPPORT_NONNULL_ANNOTATION, ANDROIDX_NONNULL_ANNOTATION)
            .put(SUPPORT_CHECK_RESULT_ANNOTATION, ANDROIDX_CHECK_RESULT_ANNOTATION)
            .put(SUPPORT_VISIBLE_FOR_TESTING, ANDROIDX_VISIBLE_FOR_TESTING)
            .build();

    ClassName remapped = null;
    if (preferAndroidX && annotationName.startsWith("android.support.annotation")) {
      remapped = ClassName.get((TypeElement) mirror.getAnnotationType().asElement());
    } else if (!preferAndroidX && annotationName.startsWith("androidx.annotation")) {
      remapped = ClassName.get((TypeElement) mirror.getAnnotationType().asElement());
    }
    if (remapped != null && map.containsKey(remapped)) {
      return AnnotationSpec.builder(map.get(remapped)).build();
    } else {
      return AnnotationSpec.get(mirror);
    }
  }

  ClassName visibleForTesting() {
    return findAnnotationClassName(ANDROIDX_VISIBLE_FOR_TESTING, SUPPORT_VISIBLE_FOR_TESTING);
  }

  ClassName nonNull() {
    return findAnnotationClassName(ANDROIDX_NONNULL_ANNOTATION, SUPPORT_NONNULL_ANNOTATION);
  }

  ClassName checkResult() {
    return findAnnotationClassName(
        ANDROIDX_CHECK_RESULT_ANNOTATION, SUPPORT_CHECK_RESULT_ANNOTATION);
  }

  static List<ClassName> nonNulls() {
    return ImmutableList.of(
        SUPPORT_NONNULL_ANNOTATION, JETBRAINS_NOTNULL_ANNOTATION, ANDROIDX_NONNULL_ANNOTATION);
  }

  private ClassName findAnnotationClassName(ClassName androidxName, ClassName supportName) {
    Elements elements = processingEnv.getElementUtils();
    TypeElement visibleForTestingTypeElement =
        elements.getTypeElement(androidxName.reflectionName());
    if (visibleForTestingTypeElement != null) {
      return androidxName;
    }

    return supportName;
  }

  List<ExecutableElement> findInstanceMethodsReturning(TypeElement clazz, TypeMirror returnType) {
    return FluentIterable.from(clazz.getEnclosedElements())
        .filter(new FilterPublicMethods(returnType, MethodType.INSTANCE))
        .transform(new ToMethod())
        .toList();
  }

  List<ExecutableElement> findInstanceMethodsReturning(TypeElement clazz, TypeElement returnType) {
    return FluentIterable.from(clazz.getEnclosedElements())
        .filter(new FilterPublicMethods(returnType, MethodType.INSTANCE))
        .transform(new ToMethod())
        .toList();
  }

  List<ExecutableElement> findStaticMethodsReturning(TypeElement clazz, TypeElement returnType) {
    return FluentIterable.from(clazz.getEnclosedElements())
        .filter(new FilterPublicMethods(returnType, MethodType.STATIC))
        .transform(new ToMethod())
        .toList();
  }

  List<ExecutableElement> findStaticMethods(TypeElement clazz) {
    return FluentIterable.from(clazz.getEnclosedElements())
        .filter(new FilterPublicMethods((TypeMirror) null /*returnType*/, MethodType.STATIC))
        .transform(new ToMethod())
        .toList();
  }

  Set<String> findClassValuesFromAnnotationOnClassAsNames(
      Element clazz, Class<? extends Annotation> annotationClass) {
    String annotationClassName = annotationClass.getName();
    AnnotationValue excludedModuleAnnotationValue = null;
    for (AnnotationMirror annotationMirror : clazz.getAnnotationMirrors()) {
      // Two different AnnotationMirrors the same class might not be equal, so compare Strings
      // instead. This check is necessary because a given class may have multiple Annotations.
      if (!annotationClassName.equals(annotationMirror.getAnnotationType().toString())) {
        continue;
      }
      Set<? extends Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> values =
          annotationMirror.getElementValues().entrySet();
      // Excludes has only one value. If we ever change that, we'd need to iterate over all
      // values in the entry set and compare the keys to whatever our Annotation's attribute is
      // (usually value).
      if (values.size() != 1) {
        throw new IllegalArgumentException("Expected single value, but found: " + values);
      }
      excludedModuleAnnotationValue = values.iterator().next().getValue();
      if (excludedModuleAnnotationValue == null
          || excludedModuleAnnotationValue instanceof Attribute.UnresolvedClass) {
        throw new IllegalArgumentException(
            "Failed to find value for: "
                + annotationClass
                + " from mirrors: "
                + clazz.getAnnotationMirrors());
      }
    }
    if (excludedModuleAnnotationValue == null) {
      return Collections.emptySet();
    }
    Object value = excludedModuleAnnotationValue.getValue();
    if (value instanceof List) {
      List<?> values = (List<?>) value;
      Set<String> result = new HashSet<>(values.size());
      for (Object current : values) {
        result.add(getExcludedModuleClassFromAnnotationAttribute(clazz, current));
      }
      return result;
    } else {
      ClassType classType = (ClassType) value;
      return Collections.singleton(classType.toString());
    }
  }

  // We should be able to cast to Attribute.Class rather than use reflection, but there are some
  // compilers that seem to break when we do so. See #2673 for an example.
  private static String getExcludedModuleClassFromAnnotationAttribute(
      Element clazz, Object attribute) {
    if (attribute.getClass().getSimpleName().equals("UnresolvedClass")) {
      throw new IllegalArgumentException(
          "Failed to parse @Excludes for: "
              + clazz
              + ", one or more excluded Modules could not be found at compile time. Ensure that all"
              + "excluded Modules are included in your classpath.");
    }
    Method[] methods = attribute.getClass().getDeclaredMethods();
    if (methods == null || methods.length == 0) {
      throw new IllegalArgumentException(
          "Failed to parse @Excludes for: " + clazz + ", invalid exclude: " + attribute);
    }
    for (Method method : methods) {
      if (method.getName().equals("getValue")) {
        try {
          return method.invoke(attribute).toString();
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new IllegalArgumentException("Failed to parse @Excludes for: " + clazz, e);
        }
      }
    }
    throw new IllegalArgumentException("Failed to parse @Excludes for: " + clazz);
  }

  private enum MethodType {
    STATIC,
    INSTANCE
  }

  private final class FilterPublicMethods implements Predicate<Element> {
    @Nullable private final TypeMirror returnType;
    private final MethodType methodType;

    FilterPublicMethods(@Nullable TypeMirror returnType, MethodType methodType) {
      this.returnType = returnType;
      this.methodType = methodType;
    }

    FilterPublicMethods(@Nullable TypeElement returnType, MethodType methodType) {
      this(returnType != null ? returnType.asType() : null, methodType);
    }

    @Override
    public boolean apply(@Nullable Element input) {
      if (input == null
          || input.getKind() != ElementKind.METHOD
          || !input.getModifiers().contains(Modifier.PUBLIC)) {
        return false;
      }
      boolean isStatic = input.getModifiers().contains(Modifier.STATIC);
      if (methodType == MethodType.STATIC && !isStatic) {
        return false;
      } else if (methodType == MethodType.INSTANCE && isStatic) {
        return false;
      }
      ExecutableElement method = (ExecutableElement) input;
      return returnType == null || isReturnValueTypeMatching(method, returnType);
    }
  }

  boolean isReturnValueTypeMatching(ExecutableElement method, TypeElement expectedReturnType) {
    return isReturnValueTypeMatching(method, expectedReturnType.asType());
  }

  private boolean isReturnValueTypeMatching(
      ExecutableElement method, TypeMirror expectedReturnType) {
    return processingEnv.getTypeUtils().isAssignable(method.getReturnType(), expectedReturnType);
  }

  private static final class ToMethod implements Function<Element, ExecutableElement> {

    @Nullable
    @Override
    public ExecutableElement apply(@Nullable Element input) {
      return (ExecutableElement) input;
    }
  }
}
