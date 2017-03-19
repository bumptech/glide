package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.ExtendsRequestManager;
import com.bumptech.glide.annotation.GlideExtension;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Generates an implementation of {@link com.bumptech.glide.RequestManager} that contains generated
 * methods from {@link GlideExtension}s and {@link ExtendsRequestManager}.
 *
 * <p>Generated {@link com.bumptech.glide.RequestManager} implementations look like this:
 * <pre>
 * <code>
 * public final class GeneratedRequestManager extends RequestManager {
 *   GeneratedRequestManager(Glide glide, Lifecycle lifecycle, RequestManagerTreeNode treeNode) {
 *     super(glide, lifecycle, treeNode);
 *   }
 *
 *   public RequestBuilder<GifDrawable> asGif() {
 *     RequestBuilder<GifDrawable> requestBuilder = this.as(GifDrawable.class);
 *     GifOptions.asGif(requestBuilder);
 *     return requestBuilder;
 *   }
 * }
 * </code>
 * </pre>
 */
final class RequestManagerGenerator {
  private static final String GLIDE_QUALIFIED_NAME =
      "com.bumptech.glide.Glide";
  private static final String REQUEST_BUILDER_QUALIFIED_NAME =
      "com.bumptech.glide.RequestBuilder";
  private static final String REQUEST_MANAGER_QUALIFIED_NAME =
      "com.bumptech.glide.RequestManager";
  private static final String LIFECYCLE_QUALIFIED_NAME =
      "com.bumptech.glide.manager.Lifecycle";
  private static final String REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME =
      "com.bumptech.glide.manager.RequestManagerTreeNode";

  static final String GENERATED_REQUEST_MANAGER_PACKAGE_NAME =
      "com.bumptech.glide";
  private static final String GENERATED_REQUEST_MANAGER_SIMPLE_NAME =
      "GlideRequests";


  private ProcessingEnvironment processingEnv;
  private final ProcessorUtil processorUtil;
  private final ClassName requestBuilderClassName;
  private final ClassName requestManagerClassName;
  private final TypeElement lifecycleType;
  private final TypeElement requestManagerTreeNodeType;
  private final TypeElement glideType;

  RequestManagerGenerator(ProcessingEnvironment processingEnv, ProcessorUtil processorUtil) {
    this.processingEnv = processingEnv;
    this.processorUtil = processorUtil;

    Elements elementUtils = processingEnv.getElementUtils();

    TypeElement requestBuilderType = elementUtils.getTypeElement(REQUEST_BUILDER_QUALIFIED_NAME);
    requestBuilderClassName = ClassName.get(requestBuilderType);

    TypeElement requestManagerType = elementUtils.getTypeElement(REQUEST_MANAGER_QUALIFIED_NAME);
    requestManagerClassName = ClassName.get(requestManagerType);

    lifecycleType = elementUtils.getTypeElement(LIFECYCLE_QUALIFIED_NAME);
    requestManagerTreeNodeType =
        elementUtils.getTypeElement(REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME);

    glideType = elementUtils.getTypeElement(GLIDE_QUALIFIED_NAME);
  }

  @Nullable
  TypeSpec generate(TypeSpec requestOptions, Set<String> glideExtensions) {
    List<MethodSpec> requestManagerMethods = generateRequestManagerMethods(glideExtensions);
    if (requestManagerMethods.isEmpty()) {
      return null;
    }
    return generateRequestManager(requestOptions, glideExtensions);
  }


  private TypeSpec generateRequestManager(TypeSpec requestOptions, Set<String> glideExtensions) {
     return TypeSpec.classBuilder(GENERATED_REQUEST_MANAGER_SIMPLE_NAME)
         .superclass(requestManagerClassName)
         .addJavadoc("Includes all additions from methods in {@link $T}s\n"
                 + "annotated with {@link $T}\n"
                 + "\n"
                 + "<p>Generated code, do not modify\n",
             GlideExtension.class, ExtendsRequestManager.class)
         .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
         .addMethod(MethodSpec.constructorBuilder()
                 .addParameter(ClassName.get(glideType), "glide")
                 .addParameter(ClassName.get(lifecycleType), "lifecycle")
                 .addParameter(ClassName.get(requestManagerTreeNodeType), "treeNode")
                 .addStatement("super(glide, lifecycle, treeNode)")
                 .build())
         .addMethods(generateRequestManagerMethods(glideExtensions))
         .addMethod(generateOverrideSetRequestOptions(requestOptions))
         .build();
  }

  private List<MethodSpec> generateRequestManagerMethods(Set<String> glideExtensions) {
    List<ExecutableElement> requestManagerExtensionMethods =
        processorUtil.findAnnotatedElementsInClasses(glideExtensions, ExtendsRequestManager.class);

    return Lists.transform(requestManagerExtensionMethods,
        new Function<ExecutableElement, MethodSpec>() {
          @Override
          public MethodSpec apply(ExecutableElement input) {
            return generateRequestManagerMethod(input);
          }
        });
  }

  private MethodSpec generateRequestManagerMethod(ExecutableElement extensionMethod) {
    String returnType = processorUtil.findClassValuesFromAnnotationOnClassAsNames(extensionMethod,
        ExtendsRequestManager.class).iterator().next();
    ClassName returnTypeClassName = ClassName.bestGuess(returnType);
    ParameterizedTypeName parameterizedTypeName =
        ParameterizedTypeName.get(requestBuilderClassName, returnTypeClassName);

    return MethodSpec.methodBuilder(extensionMethod.getSimpleName().toString())
        .addModifiers(Modifier.PUBLIC)
        .returns(parameterizedTypeName)
        .addJavadoc(processorUtil.generateSeeMethodJavadoc(extensionMethod))
        .addStatement(
            "$T requestBuilder = this.as($T.class)", parameterizedTypeName, returnTypeClassName)
        .addStatement("$T.$N(requestBuilder)", extensionMethod.getEnclosingElement(),
            extensionMethod.getSimpleName())
        .addStatement("return requestBuilder")
        .build();
  }

  /**
   * The {@link com.bumptech.glide.request.BaseRequestOptions} subclass should always be our
   * generated subclass type to avoid inadvertent errors where a different subclass is applied that
   * accidentally wipes out some logic in overidden methods in our generated subclass.
   */
  private MethodSpec generateOverrideSetRequestOptions(TypeSpec generatedRequestOptions) {
    Elements elementUtils = processingEnv.getElementUtils();
    TypeElement baseRequestOptionsType =
            elementUtils.getTypeElement(
                RequestOptionsGenerator.BASE_REQUEST_OPTIONS_QUALIFIED_NAME);
    TypeElement androidNonNullType =
            elementUtils.getTypeElement("android.support.annotation.NonNull");

    // This class may have just been generated and therefore may not be found if we try to obtain
    // it via Elements, so use just the String version instead.
    String generatedRequestOptionsQualfiedName =
        RequestOptionsGenerator.GENERATED_REQUEST_OPTIONS_PACKAGE_NAME + "."
                + generatedRequestOptions.name;

    String methodName = "setRequestOptions";
    String parameterName = "toSet";

    return MethodSpec.methodBuilder(methodName)
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(
            ParameterSpec.builder(ClassName.get(baseRequestOptionsType), parameterName)
                .addAnnotation(ClassName.get(androidNonNullType))
                .build())
        .beginControlFlow("if ($N instanceof $L)",
            parameterName, generatedRequestOptionsQualfiedName)
        .addStatement("super.$N($N)", methodName, parameterName)
        .nextControlFlow("else")
        .addStatement("super.setRequestOptions(new $L().apply($N))",
            generatedRequestOptionsQualfiedName, parameterName)
        .endControlFlow()
        .build();
  }
}
