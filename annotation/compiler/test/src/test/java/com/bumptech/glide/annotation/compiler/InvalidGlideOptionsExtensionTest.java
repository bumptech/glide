package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.emptyAppModule;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.google.common.truth.Truth;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Checks assertions on {@link com.bumptech.glide.annotation.GlideExtension}s for methods annotated
 * with {@link com.bumptech.glide.annotation.GlideOption}.
 */
// Ignore warnings since most methods use assertThrows.
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JUnit4.class)
public class InvalidGlideOptionsExtensionTest {
  @Test
  public void compilation_withAnnotatedNonStaticMethod_fails() {
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            javac()
                .withProcessors(new GlideAnnotationProcessor())
                .compile(
                    emptyAppModule(),
                    JavaFileObjects.forSourceLines(
                        "Extension",
                        "package com.bumptech.glide.test;",
                        "import com.bumptech.glide.annotation.GlideExtension;",
                        "import com.bumptech.glide.annotation.GlideOption;",
                        "@GlideExtension",
                        "public class Extension {",
                        "  private Extension() {}",
                        "  @GlideOption",
                        "  public void doSomething() {}",
                        "}"));
          }
        });
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_withRequestOptionsArgInWrongOrder_fails() {
    try {
      javac()
          .withProcessors(new GlideAnnotationProcessor())
          .compile(
              emptyAppModule(),
              JavaFileObjects.forSourceLines(
                  "NonRequestOptionsFirstArgExtension",
                  "package com.bumptech.glide.test;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "import com.bumptech.glide.annotation.GlideOption;",
                  "import com.bumptech.glide.request.BaseRequestOptions;",
                  "@GlideExtension",
                  "public class NonRequestOptionsFirstArgExtension{",
                  "  private NonRequestOptionsFirstArgExtension() {}",
                  "  @GlideOption",
                  "  public static BaseRequestOptions<?> doSomething(",
                  "      Object arg1, BaseRequestOptions<?> options) {",
                  "    return options;",
                  "  }",
                  "}"));
      fail();
    } catch (RuntimeException e) {
      String message = e.getCause().getMessage();
      Truth.assertThat(message).contains("BaseRequestOptions<?> object as their first parameter");
      Truth.assertThat(message).contains("Object");
      Truth.assertThat(message).contains("NonRequestOptionsFirstArgExtension");
    }
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_withRequestOptionsArg_succeeds() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "import com.bumptech.glide.annotation.GlideOption;",
                    "import com.bumptech.glide.request.BaseRequestOptions;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  @GlideOption",
                    "  public static BaseRequestOptions<?> doSomething(",
                    "      BaseRequestOptions<?> options) {",
                    "    return options;",
                    "  }",
                    "}"));
    assertThat(compilation).succeeded();
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_withRequestOptionsArgAndOtherArg_succeeds() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "import com.bumptech.glide.annotation.GlideOption;",
                    "import com.bumptech.glide.request.BaseRequestOptions;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  @GlideOption",
                    "  public static BaseRequestOptions<?> doSomething(",
                    "      BaseRequestOptions<?> options, Object arg2) {",
                    "    return options;",
                    "  }",
                    "}"));
    assertThat(compilation).succeeded();
  }

  @Test
  public void compilation_overridingOptionWithoutAnnotationType_fails() {
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            javac()
                .withProcessors(new GlideAnnotationProcessor())
                .compile(
                    emptyAppModule(),
                    JavaFileObjects.forSourceLines(
                        "Extension",
                        "package com.bumptech.glide.test;",
                        "import com.bumptech.glide.annotation.GlideExtension;",
                        "import com.bumptech.glide.annotation.GlideOption;",
                        "import com.bumptech.glide.request.BaseRequestOptions;",
                        "@GlideExtension",
                        "public class Extension {",
                        "  private Extension() {}",
                        "  @GlideOption",
                        "  public static BaseRequestOptions<?> centerCrop(",
                        "      BaseRequestOptions<?> options) {",
                        "    return options;",
                        "  }",
                        "}"));
          }
        });
  }

  @Test
  public void compilation_withOverrideExtend_butNotOverridingMethod_fails() {
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            javac()
                .withProcessors(new GlideAnnotationProcessor())
                .compile(
                    emptyAppModule(),
                    JavaFileObjects.forSourceLines(
                        "Extension",
                        "package com.bumptech.glide.test;",
                        "import com.bumptech.glide.annotation.GlideExtension;",
                        "import com.bumptech.glide.annotation.GlideOption;",
                        "import com.bumptech.glide.request.BaseRequestOptions;",
                        "@GlideExtension",
                        "public class Extension {",
                        "  private Extension() {}",
                        "  @GlideOption(override = GlideOption.OVERRIDE_EXTEND)",
                        "  public static BaseRequestOptions<?> something(",
                        "      BaseRequestOptions<?> options) {",
                        "    return options;",
                        "  }",
                        "}"));
          }
        });
  }

  @Test
  public void compilation_withOverrideExtend_andOverridingMethod_succeeds() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "import com.bumptech.glide.annotation.GlideOption;",
                    "import com.bumptech.glide.request.BaseRequestOptions;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  @GlideOption(override = GlideOption.OVERRIDE_EXTEND)",
                    "  public static BaseRequestOptions<?> centerCrop(",
                    "      BaseRequestOptions<?> options) {",
                    "    return options;",
                    "  }",
                    "}"));
    assertThat(compilation).succeeded();
  }

  @Test
  public void compilation_withOverrideReplace_butNotOverridingMethod_fails() {
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            javac()
                .withProcessors(new GlideAnnotationProcessor())
                .compile(
                    emptyAppModule(),
                    JavaFileObjects.forSourceLines(
                        "Extension",
                        "package com.bumptech.glide.test;",
                        "import com.bumptech.glide.annotation.GlideExtension;",
                        "import com.bumptech.glide.annotation.GlideOption;",
                        "import com.bumptech.glide.request.BaseRequestOptions;",
                        "@GlideExtension",
                        "public class Extension {",
                        "  private Extension() {}",
                        "  @GlideOption(override = GlideOption.OVERRIDE_REPLACE)",
                        "  public static BaseRequestOptions<?> something(",
                        "      BaseRequestOptions<?> options) {",
                        "    return options;",
                        "  }",
                        "}"));
          }
        });
  }

  @Test
  public void compilation_withOverrideReplace_andOverridingMethod_succeeds() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "import com.bumptech.glide.annotation.GlideOption;",
                    "import com.bumptech.glide.request.BaseRequestOptions;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  @GlideOption(override = GlideOption.OVERRIDE_REPLACE)",
                    "  public static BaseRequestOptions<?> centerCrop(",
                    "      BaseRequestOptions<?> options) {",
                    "    return options;",
                    "  }",
                    "}"));
    assertThat(compilation).succeeded();
  }

  @Test
  public void compilation_withRequestOptionsReturnValue_succeeds() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import androidx.annotation.NonNull;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "import com.bumptech.glide.annotation.GlideOption;",
                    "import com.bumptech.glide.request.BaseRequestOptions;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  @NonNull",
                    "  @GlideOption",
                    "  public static BaseRequestOptions<?> doSomething(",
                    "      BaseRequestOptions<?> options) {",
                    "    return options;",
                    "  }",
                    "}"));
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Test
  public void compilation_withNonRequestOptionsReturnValue_fails() {
    try {
      javac()
          .withProcessors(new GlideAnnotationProcessor())
          .compile(
              emptyAppModule(),
              JavaFileObjects.forSourceLines(
                  "WrongReturnTypeExtension",
                  "package com.bumptech.glide.test;",
                  "import androidx.annotation.NonNull;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "import com.bumptech.glide.annotation.GlideOption;",
                  "import com.bumptech.glide.request.BaseRequestOptions;",
                  "@GlideExtension",
                  "public class WrongReturnTypeExtension {",
                  "  private WrongReturnTypeExtension() {}",
                  "  @NonNull",
                  "  @GlideOption",
                  "  public static Object doSomething(BaseRequestOptions<?> options) {",
                  "    return options;",
                  "  }",
                  "}"));
      fail();
    } catch (RuntimeException e) {
      String message = e.getCause().getMessage();
      Truth.assertThat(message)
          .contains("@GlideOption methods should return a BaseRequestOptions<?> object");
      Truth.assertThat(message).contains("Object");
      Truth.assertThat(message).contains("WrongReturnTypeExtension");
    }
  }

  @Test
  public void compilation_withMissingNonNullAnnotation_warns() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "import com.bumptech.glide.annotation.GlideOption;",
                    "import com.bumptech.glide.request.BaseRequestOptions;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  @GlideOption",
                    "  public static BaseRequestOptions<?> doSomething(",
                    "      BaseRequestOptions<?> options) {",
                    "    return options;",
                    "  }",
                    "}"));
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningCount(1);
    assertThat(compilation).hadWarningContaining("androidx.annotation.NonNull");
    assertThat(compilation).hadWarningContaining("com.bumptech.glide.test.Extension#doSomething");
  }

  @Test
  public void compilation_withNoOptionParameters_fails() {
    try {
      javac()
          .withProcessors(new GlideAnnotationProcessor())
          .compile(
              emptyAppModule(),
              JavaFileObjects.forSourceLines(
                  "MissingRequestOptionsExtension",
                  "package com.bumptech.glide.test;",
                  "import androidx.annotation.NonNull;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "import com.bumptech.glide.annotation.GlideOption;",
                  "import com.bumptech.glide.request.BaseRequestOptions;",
                  "@GlideExtension",
                  "public class MissingRequestOptionsExtension {",
                  "  private MissingRequestOptionsExtension() {}",
                  "  @NonNull",
                  "  @GlideOption",
                  "  public static BaseRequestOptions<?> doSomething() {",
                  "    return options;",
                  "  }",
                  "}"));
      fail();
    } catch (RuntimeException e) {
      String message = e.getCause().getMessage();
      Truth.assertThat(message).contains("BaseRequestOptions<?> object as their first parameter");
      Truth.assertThat(message).contains("doSomething");
      Truth.assertThat(message).contains("MissingRequestOptionsExtension");
    }
  }
}
