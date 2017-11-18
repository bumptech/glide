package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.annotation.GlideType;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

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
      throw new IllegalArgumentException("RequestOptionsExtensions must be public");
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

  private static void validateExtensionConstructor(Element element) {
    if (!element.getModifiers().contains(Modifier.PRIVATE)) {
      throw new IllegalArgumentException("RequestOptionsExtensions must be public, with private"
          + " constructors and only static methods. Found a non-private constructor");
    }
    ExecutableElement executableElement = (ExecutableElement) element;
    if (!executableElement.getParameters().isEmpty()) {
      throw new IllegalArgumentException("RequestOptionsExtensions must be public, with private"
          + " constructors and only static methods. Found parameters in the constructor");
    }
  }

  private void validateGlideOption(ExecutableElement executableElement) {
    if (returnsVoid(executableElement)) {
      validateDeprecatedGlideOption(executableElement);
    } else {
      validateNewGlideOption(executableElement);
    }
  }

  private void validateNewGlideOption(ExecutableElement executableElement) {
    validateGlideOptionParameters(executableElement);
    TypeMirror returnType = executableElement.getReturnType();
    if (!isRequestOptions(returnType)) {
      throw new IllegalArgumentException("@GlideOption methods should return a RequestOptions"
          + " object, but given: " + returnType + ". If you're using old style @GlideOption"
          + " methods, your method may have a void return type, but doing so is deprecated and"
          + " support will be removed in a future version");
    }
    validateGlideOptionOverride(executableElement);
  }

  private void validateDeprecatedGlideOption(ExecutableElement executableElement) {
    validateStaticVoid(executableElement, GlideOption.class);
    validateGlideOptionParameters(executableElement);
    validateGlideOptionOverride(executableElement);
  }

  private static void validateGlideOptionParameters(ExecutableElement executableElement) {
    if (executableElement.getParameters().isEmpty()) {
      throw new IllegalArgumentException("@GlideOption methods must take a "
          + "RequestOptions object as their first parameter, but given none");
    }
    VariableElement first = executableElement.getParameters().get(0);
    TypeMirror expected = first.asType();
    if (!isRequestOptions(expected)) {
      throw new IllegalArgumentException("@GlideOption methods must take a"
          + " RequestOptions object as their first parameter, but given: " + expected);
    }
  }

  private static boolean isRequestOptions(TypeMirror typeMirror) {
    return typeMirror.toString().equals("com.bumptech.glide.request.RequestOptions");
  }

  private void validateGlideOptionOverride(ExecutableElement element) {
    int overrideType = processorUtil.getOverrideType(element);
    boolean isOverridingRequestOptionsMethod = isMethodInRequestOptions(element);
    if (isOverridingRequestOptionsMethod && overrideType == GlideOption.OVERRIDE_NONE) {
      throw new IllegalArgumentException("Accidentally attempting to override a method in"
          + " RequestOptions. Add an 'override' value in the @GlideOption annotation"
          + " if this is intentional. Offending method: "
          + element.getEnclosingElement() + "#" + element);
    } else if (!isOverridingRequestOptionsMethod && overrideType != GlideOption.OVERRIDE_NONE) {
      throw new IllegalArgumentException("Requested to override an existing method in"
          + " RequestOptions, but no such method was found. Offending method: "
          + element.getEnclosingElement() + "#" + element);
    }
  }

  private boolean isMethodInRequestOptions(ExecutableElement toFind) {
    // toFind is a method in a GlideExtension whose first argument is a BaseRequestOptions<?> type.
    // Since we're comparing against methods in BaseRequestOptions itself, we need to drop that
    // first type.
    TypeElement requestOptionsType =
        processingEnvironment
            .getElementUtils()
            .getTypeElement(RequestOptionsGenerator.REQUEST_OPTIONS_QUALIFIED_NAME);
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
    if (returnsVoid(executableElement)) {
      validateDeprecatedGlideType(executableElement);
    } else {
      validateNewGlideType(executableElement);
    }
  }

  private void validateNewGlideType(ExecutableElement executableElement) {
    TypeMirror returnType = executableElement.getReturnType();
    if (!isRequestBuilder(returnType) || !typeMatchesExpected(returnType, executableElement)) {
      String expectedClassName = getGlideTypeValue(executableElement);
      throw new IllegalArgumentException("@GlideType methods should return a RequestBuilder<"
          + expectedClassName + "> object, but given: " + returnType + ". If you're"
          + " using old style @GlideType methods, your method may have a void return type, but"
          + " doing so is deprecated and support will be removed in a future version");
    }
    validateGlideTypeParameters(executableElement);
  }

  private String getGlideTypeValue(ExecutableElement executableElement) {
    return
        processorUtil
            .findClassValuesFromAnnotationOnClassAsNames(
                executableElement, GlideType.class).iterator().next();
  }

  private boolean typeMatchesExpected(
      TypeMirror returnType, ExecutableElement executableElement) {
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

  private static void validateDeprecatedGlideType(ExecutableElement executableElement) {
    validateStaticVoid(executableElement, GlideType.class);
    validateGlideTypeParameters(executableElement);
  }

  private static void validateGlideTypeParameters(ExecutableElement executableElement) {
    if (executableElement.getParameters().size() != 1) {
      throw new IllegalArgumentException("@GlideType methods must take a"
          + " RequestBuilder object as their first and only parameter, but given multiple for: "
      + executableElement.getEnclosingElement() + "#" + executableElement);
    }

    VariableElement first = executableElement.getParameters().get(0);
    TypeMirror argumentType = first.asType();
    if (!argumentType.toString().startsWith("com.bumptech.glide.RequestBuilder")) {
      throw new IllegalArgumentException("@GlideType methods must take a"
          + " RequestBuilder object as their first and only parameter, but given: " + argumentType);
    }
  }

  private static void validateStatic(ExecutableElement executableElement, Class<?> clazz) {
    if (!executableElement.getModifiers().contains(Modifier.STATIC)) {
      throw new IllegalArgumentException("@" + clazz.getSimpleName() + " methods must be static");
    }
  }

  private static boolean returnsVoid(ExecutableElement executableElement) {
    TypeMirror returnType = executableElement.getReturnType();
    return returnType.getKind() == TypeKind.VOID;
  }

  private static void validateVoid(ExecutableElement executableElement, Class<?> clazz) {
    if (!returnsVoid(executableElement)) {
      throw new IllegalArgumentException("@" + clazz.getSimpleName() + " methods must return void");
    }
  }

  private static void validateStaticVoid(ExecutableElement executableElement, Class<?> clazz) {
    validateStatic(executableElement, clazz);
    validateVoid(executableElement, clazz);
  }
}
