package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.GlideOption.OVERRIDE_EXTEND;
import static com.bumptech.glide.annotation.compiler.ProcessorUtil.checkResult;
import static com.bumptech.glide.annotation.compiler.ProcessorUtil.nonNull;

import com.bumptech.glide.annotation.GlideOption;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/**
 * Generates method overrides for classes that want to mix in {@link GlideOption} annotated methods
 * in Glide extensions.
 */
final class RequestOptionsExtensionGenerator {
  private TypeName containingClassName;
  private ProcessorUtil processorUtil;

  RequestOptionsExtensionGenerator(TypeName containingClassName, ProcessorUtil processorUtil) {
    this.containingClassName = containingClassName;
    this.processorUtil = processorUtil;
  }

  /**
   * Returns the set of {@link GlideOption} annotated methods in the classes that correspond to the
   * given extension class names.
   */
  List<ExecutableElement> getRequestOptionExtensionMethods(Set<String> glideExtensionClassNames) {
   return processorUtil.findAnnotatedElementsInClasses(glideExtensionClassNames, GlideOption.class);
  }

  /**
   * Returns a list containing an override {@link MethodSpec} for all {@link GlideOption} annotated
   * methods in the classes that correspond to the given extension class names.
   */
  List<MethodSpec> generateInstanceMethodsForExtensions(Set<String> glideExtensionClassNames) {
    List<ExecutableElement> requestOptionExtensionMethods =
        getRequestOptionExtensionMethods(glideExtensionClassNames);

    List<MethodSpec> result = new ArrayList<>(requestOptionExtensionMethods.size());
    for (ExecutableElement requestOptionsExtensionMethod : requestOptionExtensionMethods) {
      result.add(generateMethodsForRequestOptionsExtension(requestOptionsExtensionMethod));
    }

    return result;
  }

  private MethodSpec generateMethodsForRequestOptionsExtension(
      ExecutableElement element) {
    if (element.getReturnType().getKind() == TypeKind.VOID) {
      processorUtil.warnLog(
          "The " + element.getSimpleName() + " method annotated with @GlideOption in the "
              + element.getEnclosingElement().getSimpleName() + " @GlideExtension is using a legacy"
              + " format. Support will be removed in a future version. Please change your method"
              + " definition so that your @GlideModule annotated methods return RequestOptions"
              + " objects instead of null.");
      return generateMethodsForRequestOptionsExtensionDeprecated(element);
    } else {
      return generateMethodsForRequestOptionsExtensionNew(element);
    }
  }

  private MethodSpec generateMethodsForRequestOptionsExtensionNew(
      ExecutableElement element) {
    int overrideType = processorUtil.getOverrideType(element);

    String methodName = element.getSimpleName().toString();
    MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
        .addModifiers(Modifier.PUBLIC)
        .addJavadoc(processorUtil.generateSeeMethodJavadoc(element))
        .varargs(element.isVarArgs())
        .returns(containingClassName);

    // The 0th element is expected to be a RequestOptions object.
    List<? extends VariableElement> paramElements =
        element.getParameters().subList(1, element.getParameters().size());
    List<ParameterSpec> parameters = ProcessorUtil.getParameters(paramElements);
    builder.addParameters(parameters);

    String extensionRequestOptionsArgument;
    if (overrideType == OVERRIDE_EXTEND) {
      builder
          .addJavadoc(
              processorUtil.generateSeeMethodJavadoc(
                  containingClassName, methodName, paramElements))
          .addAnnotation(Override.class);

      List<Object> methodArgs = new ArrayList<>();
      methodArgs.add(element.getSimpleName().toString());
      StringBuilder methodLiterals = new StringBuilder();
      if (!parameters.isEmpty()) {
        for (ParameterSpec parameter : parameters) {
          methodLiterals.append("$L, ");
          methodArgs.add(parameter.name);
        }
        methodLiterals = new StringBuilder(
            methodLiterals.substring(0, methodLiterals.length() - 2));
      }
      extensionRequestOptionsArgument = CodeBlock.builder()
          .add("super.$N(" + methodLiterals + ")", methodArgs.toArray(new Object[0]))
          .build()
          .toString();
    } else {
      extensionRequestOptionsArgument = "this";
    }

    List<Object> args = new ArrayList<>();
    StringBuilder code = new StringBuilder("return ($T) $T.$L($L, ");
    args.add(containingClassName);
    args.add(ClassName.get(element.getEnclosingElement().asType()));
    args.add(element.getSimpleName().toString());
    args.add(extensionRequestOptionsArgument);
    if (!parameters.isEmpty()) {
      for (ParameterSpec parameter : parameters) {
        code.append("$L, ");
        args.add(parameter.name);
      }
    }
    code = new StringBuilder(code.substring(0, code.length() - 2));
    code.append(")");
    builder.addStatement(code.toString(), args.toArray(new Object[0]));

    builder
        .addAnnotation(checkResult())
        .addAnnotation(nonNull());

    return builder.build();
  }

  private MethodSpec generateMethodsForRequestOptionsExtensionDeprecated(
      ExecutableElement element) {
    int overrideType = processorUtil.getOverrideType(element);

    String methodName = element.getSimpleName().toString();
    MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
        .addModifiers(Modifier.PUBLIC)
        .addJavadoc(processorUtil.generateSeeMethodJavadoc(element))
        .varargs(element.isVarArgs())
        .returns(containingClassName);

    // The 0th element is expected to be a RequestOptions object.
    List<? extends VariableElement> paramElements =
        element.getParameters().subList(1, element.getParameters().size());
    List<ParameterSpec> parameters = ProcessorUtil.getParameters(paramElements);
    builder.addParameters(parameters);

    // Generates the String and list of arguments to pass in when calling this method or super.
    // IE centerCrop(context) creates methodLiterals="%L" and methodArgs=[centerCrop, context].
    List<Object> methodArgs = new ArrayList<>();
    methodArgs.add(element.getSimpleName().toString());
    StringBuilder methodLiterals = new StringBuilder();
    if (!parameters.isEmpty()) {
      for (ParameterSpec parameter : parameters) {
        methodLiterals.append("$L, ");
        methodArgs.add(parameter.name);
      }
      methodLiterals = new StringBuilder(methodLiterals.substring(0, methodLiterals.length() - 2));
    }

    builder.beginControlFlow("if (isAutoCloneEnabled())")
        .addStatement(
            "return clone().$N(" + methodLiterals + ")", methodArgs.toArray(new Object[0]))
        .endControlFlow();

    // Add the correct super() call.
    if (overrideType == OVERRIDE_EXTEND) {
      String callSuper = "super.$L(" + methodLiterals + ")";
      builder.addStatement(callSuper, methodArgs.toArray(new Object[0]))
          .addJavadoc(
              processorUtil.generateSeeMethodJavadoc(
                  containingClassName, methodName, paramElements))
          .addAnnotation(Override.class);
    }

    // Adds: <AnnotatedClass>.<thisMethodName>(RequestOptions<?>, <arg1>, <arg2>, <argN>);
    List<Object> args = new ArrayList<>();
    StringBuilder code = new StringBuilder("$T.$L($L, ");
    args.add(ClassName.get(element.getEnclosingElement().asType()));
    args.add(element.getSimpleName().toString());
    args.add("this");
    if (!parameters.isEmpty()) {
      for (ParameterSpec parameter : parameters) {
        code.append("$L, ");
        args.add(parameter.name);
      }
    }
    code = new StringBuilder(code.substring(0, code.length() - 2));
    code.append(")");
    builder.addStatement(code.toString(), args.toArray(new Object[0]));

    builder.addStatement("return this")
        .addAnnotation(checkResult())
        .addAnnotation(nonNull());

    return builder.build();
  }

}
