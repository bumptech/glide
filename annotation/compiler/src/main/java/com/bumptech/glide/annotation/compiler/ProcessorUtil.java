package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.GlideAnnotationProcessor.DEBUG;

import com.bumptech.glide.annotation.GlideExtension;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Type.ClassType;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Utilities for writing classes and logging.
 */
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
    return processingEnv.getTypeUtils().isAssignable(element.asType(),
        appGlideModuleType.asType());
  }

  boolean isLibraryGlideModule(TypeElement element) {
    return processingEnv.getTypeUtils().isAssignable(element.asType(),
        libraryGlideModuleType.asType());
  }

  boolean isExtension(TypeElement element) {
    return element.getAnnotation(GlideExtension.class) != null;
  }

  void writeIndexer(TypeSpec indexer) {
    writeClass(COMPILER_PACKAGE_NAME, indexer);
  }

  void writeClass(String packageName, TypeSpec clazz) {
    try {
      debugLog("Writing class:\n" + clazz);
      JavaFile.builder(packageName, clazz).build().writeTo(processingEnv.getFiler());
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  List<ExecutableElement> findAnnotatedElementsInClasses(
      Set<String> classNames, Class<? extends Annotation> annotationClass) {
    List<ExecutableElement> result = new ArrayList<>();
    for (String glideExtensionClassName : classNames) {
      TypeElement glideExtension = processingEnv.getElementUtils()
          .getTypeElement(glideExtensionClassName);
      for (Element element : glideExtension.getEnclosedElements()) {
        if (element.getAnnotation(annotationClass) != null) {
          result.add((ExecutableElement) element);
        }
      }
    }
    return result;
  }

  List<TypeElement> getElementsFor(
      Class<? extends Annotation> clazz, RoundEnvironment env) {
    Collection<? extends Element> annotatedElements = env.getElementsAnnotatedWith(clazz);
    return ElementFilter.typesIn(annotatedElements);
  }

  /**
   * Generates a Javadoc code block for generated methods that delegate to methods in
   * {@link GlideExtension}s.
   *
   * <p>The generated block looks something like this:
   * <pre>
   * <code>
   *   {@literal @see} com.extension.package.name.ExtensionClassName#extensionMethod(arg1, argN)
   * </code>
   * </pre>
   *
   * @param method The method from the {@link GlideExtension} annotated class that the generated
   * method this Javadoc will be attached to delegates to.
   */
  CodeBlock generateSeeMethodJavadoc(ExecutableElement method) {
    // Use the simple name of the containing type instead of just the containing type's TypeMirror
    // so that we avoid appending <CHILD> or other type arguments to the class and breaking
    // Javadoc's linking.
    // With this we get @see RequestOptions#methodName().
    // With just ClassName.get(element.getEnclosingElement().asType()), we get:
    // @see RequestOptions<CHILD>#methodName().
    return generateSeeMethodJavadoc(getJavadocSafeName(method.getEnclosingElement()),
        method.getSimpleName().toString(), method.getParameters());
  }

  /**
   * Generates a Javadoc block for generated methods that delegate to other methods.
   *
   * <p>The generated block looks something like this:
   * <pre>
   * <code>
   *     {@literal @see} com.package.ClassContainingMethod.methodSimpleName(
   *         methodParam1, methodParamN)
   * </code>
   * </pre>
   * @param nameOfClassContainingMethod The simple class name of the class containing the method
   * without any generic types like {@literal <T>}.
   * @param methodSimpleName The name of the method.
   * @param methodParameters A maybe empty list of all the parameters for the method in question.
   */
  CodeBlock generateSeeMethodJavadoc(
      TypeName nameOfClassContainingMethod, String methodSimpleName,
      List<? extends VariableElement> methodParameters) {
    return generateSeeMethodJavadocInternal(nameOfClassContainingMethod,
        methodSimpleName, Lists.transform(methodParameters,
            new Function<VariableElement, Object>() {
              @Override
              public Object apply(VariableElement input) {
                return getJavadocSafeName(input);
              }
            }));
  }

  CodeBlock generateSeeMethodJavadoc(
      TypeName nameOfClassContainingMethod, MethodSpec methodSpec) {
    return generateSeeMethodJavadocInternal(nameOfClassContainingMethod,
        methodSpec.name, Lists.transform(methodSpec.parameters,
            new Function<ParameterSpec, Object>() {
              @Override
              public Object apply(ParameterSpec input) {
                return input.type;
              }
            }));
  }

  private CodeBlock generateSeeMethodJavadocInternal(
      TypeName nameOfClassContainingMethod, String methodName,
      List<Object> safeParameterNames) {
     String javadocString = "@see $T#$L(";
    List<Object> javadocArgs = new ArrayList<>();
    javadocArgs.add(nameOfClassContainingMethod);
    javadocArgs.add(methodName);

    for (Object param : safeParameterNames) {
      javadocString += "$T, ";
      javadocArgs.add(param);
    }
    if (javadocArgs.size() > 2) {
      javadocString = javadocString.substring(0, javadocString.length() - 2);
    }
    javadocString += ")\n";
    return CodeBlock.of(javadocString, javadocArgs.toArray(new Object[0]));
  }


   /**
   * Returns a safe String to use in a Javadoc that will function in a link.
   *
   * <p>This method exists because by Javadoc doesn't handle type parameters({@literal <T>}
   * in {@literal RequestOptions<T>} for example).
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
      if (excludedModuleAnnotationValue == null) {
        throw new NullPointerException("Failed to find Excludes#value");
      }
    }
    if (excludedModuleAnnotationValue == null) {
      return Collections.emptySet();
    }
    Object value = excludedModuleAnnotationValue.getValue();
    if (value instanceof List) {
      List values = (List) value;
      Set<String> result = new HashSet<>(values.size());
      for (Object current : values) {
        Attribute.Class currentClass = (Attribute.Class) current;
        result.add(currentClass.getValue().toString());
      }
      return result;
    } else {
      ClassType classType = (ClassType) value;
      return Collections.singleton(classType.toString());
    }
  }

  private enum MethodType {
    STATIC,
    INSTANCE
  }

  private final class FilterPublicMethods implements Predicate<Element> {
    @Nullable
    private final TypeMirror returnType;
    private final MethodType methodType;

    FilterPublicMethods(@Nullable TypeMirror returnType, MethodType methodType)  {
      this.returnType = returnType;
      this.methodType = methodType;
    }

    FilterPublicMethods(@Nullable TypeElement returnType, MethodType methodType)  {
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
      if (returnType == null) {
        return true;
      }
      return isReturnValueTypeMatching(method, returnType);
    }
  }

  boolean isReturnValueTypeMatching(ExecutableElement method, TypeElement expectedReturnType) {
    return isReturnValueTypeMatching(method, expectedReturnType.asType());
  }

  private boolean isReturnValueTypeMatching(
      ExecutableElement method, TypeMirror expectedReturnType) {
    return processingEnv.getTypeUtils().isAssignable(
        method.getReturnType(), expectedReturnType);
  }

  private static final class ToMethod implements Function<Element, ExecutableElement> {

    @Nullable
    @Override
    public ExecutableElement apply(@Nullable Element input) {
      return (ExecutableElement) input;
    }
  }

}
