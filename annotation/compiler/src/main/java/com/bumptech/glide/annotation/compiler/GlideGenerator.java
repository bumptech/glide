package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideExtension;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Generates a Glide look-alike that acts as the entry point to the generated API
 * (GlideApp.with(...)).
 *
 * <p>Generated {@code com.bumptech.glide.Glide} look-alikes look like this (note that the name is
 * configurable in {@link com.bumptech.glide.annotation.GlideModule}):
 *
 * <pre>
 * <code>
 * public final class GlideApp {
 *   private GiphyGlide() {
 *   }
 *
 *   public static File getPhotoCacheDir(Context context) {
 *     return Glide.getPhotoCacheDir(context);
 *   }
 *
 *   public static File getPhotoCacheDir(Context context, String cacheName) {
 *     return Glide.getPhotoCacheDir(context, cacheName);
 *   }
 *
 *   public static Glide get(Context context) {
 *     return Glide.get(context);
 *   }
 *
 *   public static void tearDown() {
 *     Glide.tearDown();
 *   }
 *
 *   public static GeneratedRequestManager with(Context context) {
 *     return (GeneratedRequestManager) Glide.with(context);
 *   }
 *
 *   public static GeneratedRequestManager with(Activity activity) {
 *    return (GeneratedRequestManager) Glide.with(activity);
 *   }
 *
 *   public static GeneratedRequestManager with(FragmentActivity activity) {
 *     return (GeneratedRequestManager) Glide.with(activity);
 *   }
 *
 *   public static GeneratedRequestManager with(Fragment fragment) {
 *     return (GeneratedRequestManager) Glide.with(fragment);
 *   }
 *
 *   public static GeneratedRequestManager with(androidx.fragment.app.Fragment fragment) {
 *     return (GeneratedRequestManager) Glide.with(fragment);
 *   }
 * </code>
 * </pre>
 */
final class GlideGenerator {
  private static final String GLIDE_QUALIFIED_NAME = "com.bumptech.glide.Glide";

  private static final String REQUEST_MANAGER_QUALIFIED_NAME = "com.bumptech.glide.RequestManager";

  private static final String SUPPRESS_LINT_PACKAGE_NAME = "android.annotation";
  private static final String SUPPRESS_LINT_CLASS_NAME = "SuppressLint";

  private final ProcessingEnvironment processingEnv;
  private final ProcessorUtil processorUtil;
  private final TypeElement glideType;
  private final TypeElement requestManagerType;

  GlideGenerator(ProcessingEnvironment processingEnv, ProcessorUtil processorUtil) {
    this.processingEnv = processingEnv;
    this.processorUtil = processorUtil;

    Elements elementUtils = processingEnv.getElementUtils();

    requestManagerType = elementUtils.getTypeElement(REQUEST_MANAGER_QUALIFIED_NAME);

    glideType = elementUtils.getTypeElement(GLIDE_QUALIFIED_NAME);
  }

  TypeSpec generate(
      String generatedCodePackageName, String glideName, TypeSpec generatedRequestManager) {
    return TypeSpec.classBuilder(glideName)
        .addJavadoc(
            "The entry point for interacting with Glide for Applications\n"
                + "\n"
                + "<p>Includes all generated APIs from all\n"
                + "{@link $T}s in source and dependent libraries.\n"
                + "\n"
                + "<p>This class is generated and should not be modified"
                + "\n"
                + "@see $T\n",
            GlideExtension.class,
            glideType)
        .addAnnotation(
            AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", getClass().getName())
                .build())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
        .addMethods(
            generateOverridesForGlideMethods(generatedCodePackageName, generatedRequestManager))
        .build();
  }

  private List<MethodSpec> generateOverridesForGlideMethods(
      final String generatedCodePackageName, final TypeSpec generatedRequestManager) {
    return Lists.transform(
        discoverGlideMethodsToOverride(),
        new Function<ExecutableElement, MethodSpec>() {
          @Override
          public MethodSpec apply(ExecutableElement input) {
            if (isGlideWithMethod(input)) {
              return overrideGlideWithMethod(
                  generatedCodePackageName, generatedRequestManager, input);
            } else {
              return overrideGlideStaticMethod(input);
            }
          }
        });
  }

  private MethodSpec overrideGlideStaticMethod(ExecutableElement methodToOverride) {
    List<ParameterSpec> parameters = processorUtil.getParameters(methodToOverride);

    TypeElement element =
        (TypeElement) processingEnv.getTypeUtils().asElement(methodToOverride.getReturnType());

    MethodSpec.Builder builder =
        MethodSpec.methodBuilder(methodToOverride.getSimpleName().toString())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc(processorUtil.generateSeeMethodJavadoc(methodToOverride))
            .addParameters(parameters);

    addReturnAnnotations(builder, methodToOverride);

    boolean returnsValue = element != null;
    if (returnsValue) {
      builder.returns(ClassName.get(element));
    }

    StringBuilder code = new StringBuilder(returnsValue ? "return " : "");
    code.append("$T.$N(");
    List<Object> args = new ArrayList<>();
    args.add(ClassName.get(glideType));
    args.add(methodToOverride.getSimpleName());
    if (!parameters.isEmpty()) {
      for (ParameterSpec param : parameters) {
        code.append("$L, ");
        args.add(param.name);
      }
      code = new StringBuilder(code.substring(0, code.length() - 2));
    }
    code.append(")");
    builder.addStatement(code.toString(), args.toArray(new Object[0]));
    return builder.build();
  }

  private Builder addReturnAnnotations(Builder builder, ExecutableElement methodToOverride) {
    Elements elements = processingEnv.getElementUtils();
    TypeElement visibleForTestingTypeElement =
        elements.getTypeElement(processorUtil.visibleForTesting().reflectionName());
    String visibleForTestingTypeQualifiedName = visibleForTestingTypeElement.toString();

    for (AnnotationMirror mirror : methodToOverride.getAnnotationMirrors()) {
      builder.addAnnotation(AnnotationSpec.get(mirror));

      // Suppress a lint warning if we're overriding a VisibleForTesting method.
      // See #1977.
      String annotationQualifiedName = mirror.getAnnotationType().toString();
      if (annotationQualifiedName.equals(visibleForTestingTypeQualifiedName)) {
        builder.addAnnotation(
            AnnotationSpec.builder(
                    ClassName.get(SUPPRESS_LINT_PACKAGE_NAME, SUPPRESS_LINT_CLASS_NAME))
                .addMember("value", "$S", "VisibleForTests")
                .build());
      }
    }

    return builder;
  }

  private List<ExecutableElement> discoverGlideMethodsToOverride() {
    return processorUtil.findStaticMethods(glideType);
  }

  private boolean isGlideWithMethod(ExecutableElement element) {
    return processorUtil.isReturnValueTypeMatching(element, requestManagerType);
  }

  private MethodSpec overrideGlideWithMethod(
      String packageName, TypeSpec generatedRequestManager, ExecutableElement methodToOverride) {
    ClassName generatedRequestManagerClassName =
        ClassName.get(packageName, generatedRequestManager.name);
    List<ParameterSpec> parameters = processorUtil.getParameters(methodToOverride);
    Preconditions.checkArgument(
        parameters.size() == 1, "Expected size of 1, but got %s", methodToOverride);
    ParameterSpec parameter = parameters.iterator().next();

    Builder builder =
        MethodSpec.methodBuilder(methodToOverride.getSimpleName().toString())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc(processorUtil.generateSeeMethodJavadoc(methodToOverride))
            .addParameters(parameters)
            .returns(generatedRequestManagerClassName)
            .addStatement(
                "return ($T) $T.$N($L)",
                generatedRequestManagerClassName,
                glideType,
                methodToOverride.getSimpleName().toString(),
                parameter.name);

    return addReturnAnnotations(builder, methodToOverride).build();
  }
}
