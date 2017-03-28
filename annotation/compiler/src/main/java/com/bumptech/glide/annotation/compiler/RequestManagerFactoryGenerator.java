package com.bumptech.glide.annotation.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Generates an implementation of
 * {@link com.bumptech.glide.manager.RequestManagerRetriever.RequestManagerFactory} that returns a
 * generated {@link com.bumptech.glide.RequestManager} implementation.
 *
 * <p>Generated {@link com.bumptech.glide.manager.RequestManagerRetriever.RequestManagerFactory}
 * classes look like this:
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
  private static final String GLIDE_QUALIFIED_NAME =
      "com.bumptech.glide.Glide";
  private static final String LIFECYCLE_QUALIFIED_NAME =
      "com.bumptech.glide.manager.Lifecycle";
  private static final String REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME =
      "com.bumptech.glide.manager.RequestManagerTreeNode";
  private static final String REQUEST_MANAGER_FACTORY_QUALIFIED_NAME =
      "com.bumptech.glide.manager.RequestManagerRetriever.RequestManagerFactory";
  private static final String REQUEST_MANAGER_QUALIFIED_NAME =
      "com.bumptech.glide.RequestManager";

  static final String GENERATED_REQUEST_MANAGER_FACTORY_PACKAGE_NAME =
      "com.bumptech.glide";
  static final String GENERATED_REQUEST_MANAGER_FACTORY_SIMPLE_NAME =
      "GeneratedRequestManagerFactory";

  private final TypeElement glideType;
  private final TypeElement lifecycleType;
  private final TypeElement requestManagerTreeNodeType;
  private final TypeElement requestManagerFactoryInterface;
  private final ClassName requestManagerClassName;

  RequestManagerFactoryGenerator(ProcessingEnvironment processingEnv) {
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
        .addMethod(
            MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(requestManagerClassName)
                .addParameter(ClassName.get(glideType), "glide")
                .addParameter(ClassName.get(lifecycleType), "lifecycle")
                .addParameter(ClassName.get(requestManagerTreeNodeType), "treeNode")
                .addStatement(
                    "return new $T(glide, lifecycle, treeNode)",
                    ClassName.get(generatedCodePackageName, generatedRequestManagerSpec.name))
                .build()
        )
        .build();
  }
}
