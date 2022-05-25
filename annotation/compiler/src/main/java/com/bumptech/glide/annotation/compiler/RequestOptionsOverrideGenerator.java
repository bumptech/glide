package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.RequestOptionsGenerator.BASE_REQUEST_OPTIONS_QUALIFIED_NAME;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * Generates overrides for BaseRequestOptions methods so that subclasses' methods return the
 * subclass type, not just BaseRequestOptions.
 */
final class RequestOptionsOverrideGenerator {

  private final TypeElement baseRequestOptionsType;
  private ProcessorUtil processorUtil;

  RequestOptionsOverrideGenerator(
      ProcessingEnvironment processingEnv, ProcessorUtil processorUtil) {

    this.processorUtil = processorUtil;
    baseRequestOptionsType =
        processingEnv.getElementUtils().getTypeElement(BASE_REQUEST_OPTIONS_QUALIFIED_NAME);
  }

  List<MethodSpec> generateInstanceMethodOverridesForRequestOptions(TypeName typeToOverrideIn) {
    return generateInstanceMethodOverridesForRequestOptions(
        typeToOverrideIn, Collections.<String>emptySet());
  }

  List<MethodSpec> generateInstanceMethodOverridesForRequestOptions(
      final TypeName typeToOverrideIn, final Set<String> excludedMethods) {
    return FluentIterable.from(
            processorUtil.findInstanceMethodsReturning(
                baseRequestOptionsType, baseRequestOptionsType))
        .filter(
            new Predicate<ExecutableElement>() {
              @Override
              public boolean apply(ExecutableElement input) {
                return !excludedMethods.contains(input.getSimpleName().toString());
              }
            })
        .transform(
            new Function<ExecutableElement, MethodSpec>() {
              @Override
              public MethodSpec apply(ExecutableElement input) {
                return generateRequestOptionOverride(typeToOverrideIn, input);
              }
            })
        .toList();
  }

  private MethodSpec generateRequestOptionOverride(
      TypeName typeToOverrideIn, ExecutableElement methodToOverride) {
    MethodSpec.Builder result =
        processorUtil.overriding(methodToOverride).returns(typeToOverrideIn);
    result.addCode(
        CodeBlock.builder()
            .add("return ($T) super.$N(", typeToOverrideIn, methodToOverride.getSimpleName())
            .add(
                FluentIterable.from(result.build().parameters)
                    .transform(
                        new Function<ParameterSpec, String>() {
                          @Override
                          public String apply(ParameterSpec input) {
                            return input.name;
                          }
                        })
                    .join(Joiner.on(", ")))
            .add(");\n")
            .build());

    if (methodToOverride.getSimpleName().toString().contains("transform")
        && methodToOverride.isVarArgs()) {
      result
          .addModifiers(Modifier.FINAL)
          .addAnnotation(SafeVarargs.class)
          .addAnnotation(
              AnnotationSpec.builder(SuppressWarnings.class)
                  .addMember("value", "$S", "varargs")
                  .build());
    }

    for (AnnotationMirror mirror : methodToOverride.getAnnotationMirrors()) {
      result.addAnnotation(AnnotationSpec.get(mirror));
    }

    return result.build();
  }
}
