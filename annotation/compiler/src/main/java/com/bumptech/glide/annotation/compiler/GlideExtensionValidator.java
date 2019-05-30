package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.ProcessorUtil.nonNulls;

import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.annotation.GlideType;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.squareup.javapoet.ClassName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

/**
 * Validates that classes annotated with {@link com.bumptech.glide.annotation.GlideExtension}
 * contains methods with the expected format.
 *
 * <p>Validation is performed so that errors can be found when a library is compiled. Without
 * validation, an error written in to a library wouldn't be found until Glide tried to generate code
 * for an Application.
 */
final class GlideExtensionValidator {
  private final ProcessingEnvironment processingEnvironment;
  private final ProcessorUtil processorUtil;

  GlideExtensionValidator(
      ProcessingEnvironment processingEnvironment, ProcessorUtil processorUtil) {
    this.processingEnvironment = processingEnvironment;
    this.processorUtil = processorUtil;
  }

  void validateExtension(TypeElement typeElement) {
    if (!typeElement.getModifiers().contains(Modifier.PUBLIC)) {
      throw new IllegalArgumentException(
          "RequestOptionsExtensions must be public, including: " + getName(typeElement));
    }
    for (Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() == ElementKind.CONSTRUCTOR) {
        validateExtensionConstructor(element);
      } else if (element.getKind() == ElementKind.METHOD) {
        ExecutableElement executableElement = (ExecutableElement) element;
        if (executableElement.getAnnotation(GlideOption.class) != null) {
          validateGlideOption(executableElement);
        } else if (executableElement.getAnnotation(GlideType.class) != null) {
          validateGlideType(executableElement);
        }
      }
    }
  }

  private static String getQualifiedMethodName(ExecutableElement executableElement) {
    return getEnclosingClassName(executableElement) + "#" + getName(executableElement);
  }

  private static String getEnclosingClassName(Element element) {
    return element.getEnclosingElement().toString();
  }

  private static String getName(Element element) {
    return element.toString();
  }

  private static void validateExtensionConstructor(Element element) {
    if (!element.getModifiers().contains(Modifier.PRIVATE)) {
      throw new IllegalArgumentException(
          "RequestOptionsExtensions must be public, with private constructors and only static"
              + " methods. Found a non-private constructor in: "
              + getEnclosingClassName(element));
    }
    ExecutableElement executableElement = (ExecutableElement) element;
    if (!executableElement.getParameters().isEmpty()) {
      throw new IllegalArgumentException(
          "RequestOptionsExtensions must be public, with private constructors and only static"
              + " methods. Found parameters in the constructor of: "
              + getEnclosingClassName(element));
    }
  }

  private void validateGlideOption(ExecutableElement executableElement) {
    validateGlideOptionAnnotations(executableElement);
    validateGlideOptionParameters(executableElement);
    TypeMirror returnType = executableElement.getReturnType();
    if (!isBaseRequestOptions(returnType)) {
      throw new IllegalArgumentException(
          "@GlideOption methods should return a"
              + " BaseRequestOptions<?> object, but "
              + getQualifiedMethodName(executableElement)
              + " returns "
              + returnType
              + ". If you're using old style @GlideOption methods, your"
              + " method may have a void return type, but doing so is deprecated and support will"
              + " be removed in a future version");
    }
    validateGlideOptionOverride(executableElement);
  }

  private void validateGlideOptionAnnotations(ExecutableElement executableElement) {
    validateAnnotatedNonNull(executableElement);
  }

  private static void validateGlideOptionParameters(ExecutableElement executableElement) {
    if (executableElement.getParameters().isEmpty()) {
      throw new IllegalArgumentException(
          "@GlideOption methods must take a "
              + "BaseRequestOptions<?> object as their first parameter, but "
              + getQualifiedMethodName(executableElement)
              + " has none");
    }
    VariableElement first = executableElement.getParameters().get(0);
    TypeMirror expected = first.asType();
    if (!isBaseRequestOptions(expected)) {
      throw new IllegalArgumentException(
          "@GlideOption methods must take a"
              + " BaseRequestOptions<?> object as their first parameter, but the first parameter"
              + " in "
              + getQualifiedMethodName(executableElement)
              + " is "
              + expected);
    }
  }

  private static boolean isBaseRequestOptions(TypeMirror typeMirror) {
    return typeMirror.toString().equals("com.bumptech.glide.request.BaseRequestOptions<?>");
  }

  private void validateGlideOptionOverride(ExecutableElement element) {
    int overrideType = processorUtil.getOverrideType(element);
    boolean isOverridingBaseRequestOptionsMethod = isMethodInBaseRequestOptions(element);
    if (isOverridingBaseRequestOptionsMethod && overrideType == GlideOption.OVERRIDE_NONE) {
      throw new IllegalArgumentException(
          "Accidentally attempting to override a method in"
              + " BaseRequestOptions. Add an 'override' value in the @GlideOption annotation"
              + " if this is intentional. Offending method: "
              + getQualifiedMethodName(element));
    } else if (!isOverridingBaseRequestOptionsMethod && overrideType != GlideOption.OVERRIDE_NONE) {
      throw new IllegalArgumentException(
          "Requested to override an existing method in"
              + " BaseRequestOptions, but no such method was found. Offending method: "
              + getQualifiedMethodName(element));
    }
  }

  private boolean isMethodInBaseRequestOptions(ExecutableElement toFind) {
    // toFind is a method in a GlideExtension whose first argument is a BaseRequestOptions<?> type.
    // Since we're comparing against methods in BaseRequestOptions itself, we need to drop that
    // first type.
    TypeElement requestOptionsType =
        processingEnvironment
            .getElementUtils()
            .getTypeElement(RequestOptionsGenerator.BASE_REQUEST_OPTIONS_QUALIFIED_NAME);
    List<String> toFindParameterNames = getComparableParameterNames(toFind, true /*skipFirst*/);
    String toFindSimpleName = toFind.getSimpleName().toString();
    for (Element element : requestOptionsType.getEnclosedElements()) {
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

  private void validateGlideType(ExecutableElement executableElement) {
    TypeMirror returnType = executableElement.getReturnType();
    validateGlideTypeAnnotations(executableElement);
    if (!isRequestBuilder(returnType) || !typeMatchesExpected(returnType, executableElement)) {
      String expectedClassName = getGlideTypeValue(executableElement);
      throw new IllegalArgumentException(
          "@GlideType methods should return a RequestBuilder<"
              + expectedClassName
              + "> object, but "
              + getQualifiedMethodName(executableElement)
              + " returns: "
              + returnType
              + ". If you're using old style @GlideType methods, your"
              + " method may have a void return type, but doing so is deprecated and support will"
              + " be removed in a future version");
    }
    validateGlideTypeParameters(executableElement);
  }

  private String getGlideTypeValue(ExecutableElement executableElement) {
    return processorUtil
        .findClassValuesFromAnnotationOnClassAsNames(executableElement, GlideType.class)
        .iterator()
        .next();
  }

  private boolean typeMatchesExpected(TypeMirror returnType, ExecutableElement executableElement) {
    if (!(returnType instanceof DeclaredType)) {
      return false;
    }
    List<? extends TypeMirror> typeArguments = ((DeclaredType) returnType).getTypeArguments();
    if (typeArguments.size() != 1) {
      return false;
    }
    TypeMirror argument = typeArguments.get(0);
    String expected = getGlideTypeValue(executableElement);
    return argument.toString().equals(expected);
  }

  private boolean isRequestBuilder(TypeMirror typeMirror) {
    TypeMirror toCompare = processingEnvironment.getTypeUtils().erasure(typeMirror);
    return toCompare.toString().equals("com.bumptech.glide.RequestBuilder");
  }

  private static void validateGlideTypeParameters(ExecutableElement executableElement) {
    if (executableElement.getParameters().size() != 1) {
      throw new IllegalArgumentException(
          "@GlideType methods must take a"
              + " RequestBuilder object as their first and only parameter, but given multiple for: "
              + getQualifiedMethodName(executableElement));
    }

    VariableElement first = executableElement.getParameters().get(0);
    TypeMirror argumentType = first.asType();
    if (!argumentType.toString().startsWith("com.bumptech.glide.RequestBuilder")) {
      throw new IllegalArgumentException(
          "@GlideType methods must take a"
              + " RequestBuilder object as their first and only parameter, but given: "
              + argumentType
              + " for: "
              + getQualifiedMethodName(executableElement));
    }
  }

  private void validateGlideTypeAnnotations(ExecutableElement executableElement) {
    validateAnnotatedNonNull(executableElement);
  }

  private void validateAnnotatedNonNull(ExecutableElement executableElement) {
    Set<String> annotationNames =
        FluentIterable.from(executableElement.getAnnotationMirrors())
            .transform(
                new Function<AnnotationMirror, String>() {
                  @Override
                  public String apply(AnnotationMirror input) {
                    return input.getAnnotationType().asElement().toString();
                  }
                })
            .toSet();
    boolean noNonNull = true;
    for (ClassName nonNull : nonNulls()) {
      if (annotationNames.contains(nonNull.reflectionName())) {
        noNonNull = false;
        break;
      }
    }
    if (noNonNull) {
      processingEnvironment
          .getMessager()
          .printMessage(
              Kind.WARNING,
              getQualifiedMethodName(executableElement)
                  + " is missing the "
                  + processorUtil.nonNull().reflectionName()
                  + " annotation,"
                  + " please add it to ensure that your extension methods are always returning"
                  + " non-null values");
    }
  }
}
