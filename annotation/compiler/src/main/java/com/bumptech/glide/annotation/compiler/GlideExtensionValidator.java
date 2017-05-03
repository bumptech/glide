package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.annotation.GlideType;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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

  private GlideExtensionValidator() { }

  static void validateExtension(TypeElement typeElement) {
    if (!typeElement.getModifiers().contains(Modifier.PUBLIC)) {
      throw new IllegalArgumentException("RequestOptionsExtensions must be public");
    }
    for (Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() == ElementKind.CONSTRUCTOR) {
        if (!element.getModifiers().contains(Modifier.PRIVATE)) {
          throw new IllegalArgumentException("RequestOptionsExtensions must be public, with private"
              + " constructors and only static methods. Found a non-private constructor");
        }
        ExecutableElement executableElement = (ExecutableElement) element;
        if (!executableElement.getParameters().isEmpty()) {
          throw new IllegalArgumentException("RequestOptionsExtensions must be public, with private"
              + " constructors and only static methods. Found parameters in the constructor");
        }
        continue;
      }
      if (element.getKind() == ElementKind.METHOD) {
        ExecutableElement executableElement = (ExecutableElement) element;
        if (executableElement.getAnnotation(GlideOption.class) != null) {
          validateExtendsRequestOptions(executableElement);
        } else if (executableElement.getAnnotation(GlideType.class) != null) {
          validateExtendsRequestManager(executableElement);
        }
      }
    }
  }

  private static void validateExtendsRequestOptions(ExecutableElement executableElement) {
    validateStaticVoid(executableElement, GlideOption.class);
    if (executableElement.getParameters().isEmpty()) {
      throw new IllegalArgumentException("@GlideOption methods must take a "
          + "RequestOptions object as their first parameter, but given none");
    }
    VariableElement first = executableElement.getParameters().get(0);
    TypeMirror expected = first.asType();
    if (!expected.toString().equals(
        "com.bumptech.glide.request.RequestOptions")) {
      throw new IllegalArgumentException("@GlideOption methods must take a"
          + " RequestOptions object as their first parameter, but given: " + expected);
    }
  }

  private static void validateExtendsRequestManager(ExecutableElement executableElement) {
    validateStaticVoid(executableElement, GlideType.class);
    if (executableElement.getParameters().size() != 1) {
      throw new IllegalArgumentException("@GlideType methods must take a"
          + " RequestOptions object as their first and only parameter, found multiple for: "
      + executableElement.getEnclosingElement() + "#" + executableElement);
    }

    VariableElement first = executableElement.getParameters().get(0);
    TypeMirror expected = first.asType();
    if (!expected.toString().startsWith("com.bumptech.glide.RequestBuilder")) {
      throw new IllegalArgumentException("@GlideType methods must take a"
          + " RequestBuilder object as their first parameter, but given: " + expected);
    }
  }

  private static void validateStaticVoid(ExecutableElement executableElement, Class<?> clazz) {
    if (!executableElement.getModifiers().contains(Modifier.STATIC)) {
      throw new IllegalArgumentException("@" + clazz.getSimpleName() + " methods must be static");
    }
    TypeMirror returnType = executableElement.getReturnType();
    if (returnType.getKind() != TypeKind.VOID) {
      throw new IllegalArgumentException("@" + clazz.getSimpleName() + " methods must return void");
    }
  }
}
