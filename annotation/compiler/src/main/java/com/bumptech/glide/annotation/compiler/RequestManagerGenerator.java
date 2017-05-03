package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideType;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

/**
 * Generates an implementation of {@link com.bumptech.glide.RequestManager} that contains generated
 * methods from {@link GlideExtension}s and {@link GlideType}.
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
  private static final String REQUEST_MANAGER_QUALIFIED_NAME =
      "com.bumptech.glide.RequestManager";
  private static final String LIFECYCLE_QUALIFIED_NAME =
      "com.bumptech.glide.manager.Lifecycle";
  private static final String REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME =
      "com.bumptech.glide.manager.RequestManagerTreeNode";

  private static final String GENERATED_REQUEST_MANAGER_SIMPLE_NAME =
      "GlideRequests";

  private ProcessingEnvironment processingEnv;
  private final ProcessorUtil processorUtil;
  private final ClassName requestManagerClassName;
  private final TypeElement lifecycleType;
  private final TypeElement requestManagerTreeNodeType;
  private final TypeElement glideType;
  private final TypeElement requestManagerType;
  private final TypeElement requestBuilderType;
  private ClassName generatedRequestBuilderClassName;

  RequestManagerGenerator(ProcessingEnvironment processingEnv, ProcessorUtil processorUtil) {
    this.processingEnv = processingEnv;
    this.processorUtil = processorUtil;

    Elements elementUtils = processingEnv.getElementUtils();

    requestManagerType = elementUtils.getTypeElement(REQUEST_MANAGER_QUALIFIED_NAME);
    requestManagerClassName = ClassName.get(requestManagerType);

    lifecycleType = elementUtils.getTypeElement(LIFECYCLE_QUALIFIED_NAME);
    requestManagerTreeNodeType =
        elementUtils.getTypeElement(REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME);

    requestBuilderType =
        elementUtils.getTypeElement(RequestBuilderGenerator.REQUEST_BUILDER_QUALIFIED_NAME);

    glideType = elementUtils.getTypeElement(GLIDE_QUALIFIED_NAME);
  }

  @Nullable
  TypeSpec generate(
      String generatedCodePackageName, @Nullable TypeSpec requestOptions, TypeSpec requestBuilder,
      Set<String> glideExtensions) {
    generatedRequestBuilderClassName = ClassName.get(generatedCodePackageName, requestBuilder.name);
    return TypeSpec.classBuilder(GENERATED_REQUEST_MANAGER_SIMPLE_NAME)
         .superclass(requestManagerClassName)
         .addJavadoc("Includes all additions from methods in {@link $T}s\n"
                 + "annotated with {@link $T}\n"
                 + "\n"
                 + "<p>Generated code, do not modify\n",
             GlideExtension.class, GlideType.class)
        .addAnnotation(
            AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "deprecation")
                .build())
         .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
         .addMethod(generateAsMethod(generatedCodePackageName, requestBuilder))
         .addMethod(generateCallSuperConstructor())
         .addMethods(generateAdditionalRequestManagerMethods(glideExtensions))
         .addMethods(generateRequestManagerMethodOverrides())
         .addMethods(
             FluentIterable.from(
                 Collections.singletonList(
                     generateOverrideSetRequestOptions(generatedCodePackageName, requestOptions)))
                 .filter(Predicates.<MethodSpec>notNull()))
         .build();
  }

  private MethodSpec generateCallSuperConstructor() {
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.get(glideType), "glide")
        .addParameter(ClassName.get(lifecycleType), "lifecycle")
        .addParameter(ClassName.get(requestManagerTreeNodeType), "treeNode")
        .addStatement("super(glide, lifecycle, treeNode)")
        .build();
  }

  private MethodSpec generateAsMethod(String generatedCodePackageName, TypeSpec requestBuilder) {
    TypeVariableName resourceType = TypeVariableName.get("ResourceType");
    ParameterizedTypeName classOfResouceType = ParameterizedTypeName
        .get(ClassName.get(Class.class), resourceType);

    ClassName generatedRequestBuilderClassName =
        ClassName.get(generatedCodePackageName, requestBuilder.name);

    ParameterizedTypeName requestBuilderOfResourceType = ParameterizedTypeName
        .get(generatedRequestBuilderClassName, resourceType);

    return MethodSpec.methodBuilder("as")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addTypeVariable(TypeVariableName.get("ResourceType"))
        .addParameter(classOfResouceType, "resourceClass")
        .returns(requestBuilderOfResourceType)
        .addStatement("return new $T<>(glide, this, resourceClass)",
            this.generatedRequestBuilderClassName)
        .build();
  }

  private List<MethodSpec> generateRequestManagerMethodOverrides() {
    // Without the erasure, this is a RequestBuilder<Y>. A RequestBuilder<X> is not assignable to a
    // RequestBuilder<Y>. After type erasure this is a RequestBuilder. A RequestBuilder<X> is
    // assignable to the raw RequestBuilder.
    TypeMirror rawRequestBuilder = processingEnv.getTypeUtils()
        .erasure(requestBuilderType.asType());

    final TypeElement classType =
        processingEnv.getElementUtils().getTypeElement(Class.class.getCanonicalName());
    final TypeMirror rawClassType = processingEnv.getTypeUtils().erasure(classType.asType());

    return FluentIterable.from(
        processorUtil.findInstanceMethodsReturning(requestManagerType, rawRequestBuilder))
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement input) {
            // Skip the <T> as(Class<T>) method.
            return !input.getSimpleName().toString().equals("as")
                || input.getParameters().size() != 1
                || !processingEnv.getTypeUtils().isAssignable(
                    input.getParameters().get(0).asType(), rawClassType);
          }
        })
        .transform(new Function<ExecutableElement, MethodSpec>() {
          @Override
          public MethodSpec apply(ExecutableElement input) {
            return generateRequestManagerMethodOverride(input);
          }
        })
        .toList();
  }

  /**
   * Generates overrides of existing RequestManager methods so that they return our generated
   * RequestBuilder subtype.
   */
  private MethodSpec generateRequestManagerMethodOverride(ExecutableElement methodToOverride) {
     // We've already verified that this method returns a RequestBuilder and RequestBuilders have
    // exactly one type argument, so this is safe unless those assumptions change.
    TypeMirror typeArgument =
        ((DeclaredType) methodToOverride.getReturnType()).getTypeArguments().get(0);

    ParameterizedTypeName generatedRequestBuilderOfType =
        ParameterizedTypeName.get(generatedRequestBuilderClassName, ClassName.get(typeArgument));

    return MethodSpec.overriding(methodToOverride)
        .returns(generatedRequestBuilderOfType)
        .addCode(CodeBlock.builder()
            .add("return ($T) super.$N(",
                generatedRequestBuilderOfType, methodToOverride.getSimpleName())
            .add(FluentIterable.from(methodToOverride.getParameters())
                .transform(new Function<VariableElement, String>() {
                  @Override
                  public String apply(VariableElement input) {
                    return input.getSimpleName().toString();
                  }
                })
                .join(Joiner.on(", ")))
            .add(");\n")
            .build())
        .build();
  }

  private List<MethodSpec> generateAdditionalRequestManagerMethods(
      Set<String> glideExtensions) {
    List<ExecutableElement> requestManagerExtensionMethods =
        processorUtil.findAnnotatedElementsInClasses(glideExtensions, GlideType.class);

    return Lists.transform(requestManagerExtensionMethods,
        new Function<ExecutableElement, MethodSpec>() {
          @Override
          public MethodSpec apply(ExecutableElement input) {
            return generateAdditionalRequestManagerMethod(input);
          }
        });
  }

  // Generates methods added to RequestManager via GlideExtensions.
  private MethodSpec generateAdditionalRequestManagerMethod(ExecutableElement extensionMethod) {
    String returnType = processorUtil.findClassValuesFromAnnotationOnClassAsNames(extensionMethod,
        GlideType.class).iterator().next();
    ClassName returnTypeClassName = ClassName.bestGuess(returnType);
    ParameterizedTypeName parameterizedTypeName =
        ParameterizedTypeName.get(generatedRequestBuilderClassName, returnTypeClassName);

    return MethodSpec.methodBuilder(extensionMethod.getSimpleName().toString())
        .addModifiers(Modifier.PUBLIC)
        .returns(parameterizedTypeName)
        .addJavadoc(processorUtil.generateSeeMethodJavadoc(extensionMethod))
        .addStatement(
            "$T requestBuilder = this.as($T.class)", parameterizedTypeName, returnTypeClassName)
        .addStatement("$T.$N(requestBuilder)",
            extensionMethod.getEnclosingElement(), extensionMethod.getSimpleName())
        .addStatement("return requestBuilder")
        .build();
  }

  /**
   * The {@link com.bumptech.glide.request.RequestOptions} subclass should always be our
   * generated subclass type to avoid inadvertent errors where a different subclass is applied that
   * accidentally wipes out some logic in overidden methods in our generated subclass.
   */
  @Nullable
  private MethodSpec generateOverrideSetRequestOptions(
      String generatedCodePackageName, @Nullable TypeSpec generatedRequestOptions) {
    if (generatedRequestOptions == null) {
      return null;
    }

    Elements elementUtils = processingEnv.getElementUtils();
    TypeElement requestOptionsType =
            elementUtils.getTypeElement(
                RequestOptionsGenerator.REQUEST_OPTIONS_QUALIFIED_NAME);
    TypeElement androidNonNullType =
            elementUtils.getTypeElement("android.support.annotation.NonNull");

    // This class may have just been generated and therefore may not be found if we try to obtain
    // it via Elements, so use just the String version instead.
    String generatedRequestOptionsQualifiedName =
        generatedCodePackageName + "." + generatedRequestOptions.name;

    String methodName = "setRequestOptions";
    String parameterName = "toSet";

    return MethodSpec.methodBuilder(methodName)
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(
            ParameterSpec.builder(ClassName.get(requestOptionsType), parameterName)
                .addAnnotation(ClassName.get(androidNonNullType))
                .build())
        .beginControlFlow("if ($N instanceof $L)",
            parameterName, generatedRequestOptionsQualifiedName)
        .addStatement("super.$N($N)", methodName, parameterName)
        .nextControlFlow("else")
        .addStatement("super.setRequestOptions(new $L().apply($N))",
            generatedRequestOptionsQualifiedName, parameterName)
        .endControlFlow()
        .build();
  }
}
