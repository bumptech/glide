package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideExtension;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * Generates a Glide look-alike that acts as the entry point to the generated API
 * (GlideApp.with(...)).
 *
 * <p>>Generated {@link com.bumptech.glide.Glide} look-alikes look like this (note that the name
 * is configurable in {@link com.bumptech.glide.annotation.GlideModule}):
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
 *   public static GeneratedRequestManager with(android.support.v4.app.Fragment fragment) {
 *     return (GeneratedRequestManager) Glide.with(fragment);
 *   }
 * </code>
 * </pre>
 */
final class GlideGenerator {
  private static final String GLIDE_QUALIFIED_NAME =
      "com.bumptech.glide.Glide";

  private static final String REQUEST_MANAGER_QUALIFIED_NAME =
      "com.bumptech.glide.RequestManager";

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
                + "@see $T\n", GlideExtension.class, glideType)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build())
        .addMethods(
            generateOverridesForGlideMethods(generatedCodePackageName, generatedRequestManager))
        .build();
  }

  private List<MethodSpec> generateOverridesForGlideMethods(
      final String generatedCodePackageName, final TypeSpec generatedRequestManager) {
    return Lists.transform(discoverGlideMethodsToOverride(),
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
    List<? extends VariableElement> parameters = methodToOverride.getParameters();

    TypeElement element =
        (TypeElement) processingEnv.getTypeUtils().asElement(methodToOverride.getReturnType());

    MethodSpec.Builder builder =
        MethodSpec.methodBuilder(methodToOverride.getSimpleName().toString())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc(processorUtil.generateSeeMethodJavadoc(methodToOverride))
            .addParameters(Lists.transform(parameters,
                new Function<VariableElement, ParameterSpec>() {
                  @Override
                  public ParameterSpec apply(VariableElement input) {
                    return ParameterSpec.get(input);
                  }
            }));

    boolean returnsValue = element != null;
    if (returnsValue) {
      builder.returns(ClassName.get(element));
    }

    String code = returnsValue ? "return " : "";
    code += "$T.$N(";
    List<Object> args = new ArrayList<>();
    args.add(ClassName.get(glideType));
    args.add(methodToOverride.getSimpleName());
    if (!parameters.isEmpty()) {
      for (VariableElement param : parameters) {
        code += "$L, ";
        args.add(param.getSimpleName());
      }
      code = code.substring(0, code.length() - 2);
    }
    code += ")";
    builder.addStatement(code, args.toArray(new Object[0]));
    return builder.build();
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
    List<? extends VariableElement> parameters = methodToOverride.getParameters();
    Preconditions.checkArgument(
        parameters.size() == 1, "Expected size of 1, but got %s", methodToOverride);
    VariableElement parameter = parameters.iterator().next();
    return MethodSpec.methodBuilder(methodToOverride.getSimpleName().toString())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addJavadoc(processorUtil.generateSeeMethodJavadoc(methodToOverride))
        .returns(generatedRequestManagerClassName)
        .addParameter(ClassName.get(parameter.asType()), parameter.getSimpleName().toString())
        .addStatement("return ($T) $T.$N($L)",
            generatedRequestManagerClassName, glideType,
            methodToOverride.getSimpleName().toString(),
            parameter.getSimpleName())
        .build();
  }
}
