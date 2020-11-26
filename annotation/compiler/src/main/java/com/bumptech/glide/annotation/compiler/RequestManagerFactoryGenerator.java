package com.bumptech.glide.annotation.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import javax.annotation.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Generates an implementation of {@code
 * com.bumptech.glide.manager.RequestManagerRetriever.RequestManagerFactory} that returns a
 * generated {@code com.bumptech.glide.RequestManager} implementation.
 *
 * <p>Generated {@code com.bumptech.glide.manager.RequestManagerRetriever.RequestManagerFactory}
 * classes look like this:
 *
 * <pre>
 * <code>
 * public class GeneratedRequestManagerFactory
 *     implements RequestManagerRetriever.RequestManagerFactory {
 *   {@literal @Override}
 *   public RequestManager build(Glide glide, Lifecycle lifecycle,
 *       RequestManagerTreeNode treeNode) {
 *     return new GeneratedRequestManager(glide, lifecycle, treeNode);
 *   }
 * }
 * </code>
 * </pre>
 */
final class RequestManagerFactoryGenerator {
  private static final String GLIDE_QUALIFIED_NAME = "com.bumptech.glide.Glide";
  private static final String LIFECYCLE_QUALIFIED_NAME = "com.bumptech.glide.manager.Lifecycle";
  private static final String REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME =
      "com.bumptech.glide.manager.RequestManagerTreeNode";
  private static final String REQUEST_MANAGER_FACTORY_QUALIFIED_NAME =
      "com.bumptech.glide.manager.RequestManagerRetriever.RequestManagerFactory";
  private static final String REQUEST_MANAGER_QUALIFIED_NAME = "com.bumptech.glide.RequestManager";
  private static final ClassName CONTEXT_CLASS_NAME = ClassName.get("android.content", "Context");

  static final String GENERATED_REQUEST_MANAGER_FACTORY_PACKAGE_NAME = "com.bumptech.glide";
  static final String GENERATED_REQUEST_MANAGER_FACTORY_SIMPLE_NAME =
      "GeneratedRequestManagerFactory";

  private final TypeElement glideType;
  private final TypeElement lifecycleType;
  private final TypeElement requestManagerTreeNodeType;
  private final TypeElement requestManagerFactoryInterface;
  private final ClassName requestManagerClassName;
  private final ProcessorUtil processorUtil;

  RequestManagerFactoryGenerator(ProcessingEnvironment processingEnv, ProcessorUtil processorUtil) {
    this.processorUtil = processorUtil;
    Elements elementUtils = processingEnv.getElementUtils();
    glideType = elementUtils.getTypeElement(GLIDE_QUALIFIED_NAME);
    lifecycleType = elementUtils.getTypeElement(LIFECYCLE_QUALIFIED_NAME);
    requestManagerTreeNodeType =
        elementUtils.getTypeElement(REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME);

    requestManagerFactoryInterface =
        elementUtils.getTypeElement(REQUEST_MANAGER_FACTORY_QUALIFIED_NAME);

    TypeElement requestManagerType = elementUtils.getTypeElement(REQUEST_MANAGER_QUALIFIED_NAME);
    requestManagerClassName = ClassName.get(requestManagerType);
  }

  TypeSpec generate(String generatedCodePackageName, TypeSpec generatedRequestManagerSpec) {
    return TypeSpec.classBuilder(GENERATED_REQUEST_MANAGER_FACTORY_SIMPLE_NAME)
        .addModifiers(Modifier.FINAL)
        .addSuperinterface(ClassName.get(requestManagerFactoryInterface))
        .addJavadoc("Generated code, do not modify\n")
        .addAnnotation(
            AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", getClass().getName())
                .build())
        .addMethod(
            MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addAnnotation(processorUtil.nonNull())
                .returns(requestManagerClassName)
                .addParameter(
                    ParameterSpec.builder(ClassName.get(glideType), "glide")
                        .addAnnotation(processorUtil.nonNull())
                        .build())
                .addParameter(
                    ParameterSpec.builder(ClassName.get(lifecycleType), "lifecycle")
                        .addAnnotation(processorUtil.nonNull())
                        .build())
                .addParameter(
                    ParameterSpec.builder(ClassName.get(requestManagerTreeNodeType), "treeNode")
                        .addAnnotation(processorUtil.nonNull())
                        .build())
                .addParameter(
                    ParameterSpec.builder(CONTEXT_CLASS_NAME, "context")
                        .addAnnotation(processorUtil.nonNull())
                        .build())
                .addStatement(
                    "return new $T(glide, lifecycle, treeNode, context)",
                    ClassName.get(generatedCodePackageName, generatedRequestManagerSpec.name))
                .build())
        .build();
  }
}
