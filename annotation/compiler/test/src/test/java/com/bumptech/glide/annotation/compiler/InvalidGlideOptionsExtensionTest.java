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
          public void run() throws Throwable {
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
                  "import com.bumptech.glide.request.RequestOptions;",
                  "@GlideExtension",
                  "public class NonRequestOptionsFirstArgExtension{",
                  "  private NonRequestOptionsFirstArgExtension() {}",
                  "  @GlideOption",
                  "  public static void doSomething(Object arg1, RequestOptions options) {}",
                  "}"));
      fail();
    } catch (RuntimeException e) {
      String message = e.getCause().getMessage();
      Truth.assertThat(message).contains("RequestOptions object as their first parameter");
      Truth.assertThat(message).contains("Object");
      Truth.assertThat(message).contains("NonRequestOptionsFirstArgExtension");
    }
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_withRequestOptionsArg_succeeds() {
    Compilation compilation = javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideOption;",
                "import com.bumptech.glide.request.RequestOptions;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @GlideOption",
                "  public static void doSomething(RequestOptions options) {}",
                "}"));
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningContaining("is using a legacy format.");
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_withRequestOptionsArgAndOtherArg_succeeds() {
    Compilation compilation = javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideOption;",
                "import com.bumptech.glide.request.RequestOptions;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @GlideOption",
                "  public static void doSomething(RequestOptions options, Object arg2) {}",
                "}"));
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningContaining("is using a legacy format.");
  }

  @Test
  public void compilation_overridingOptionWithoutAnnotationType_fails() {
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            javac()
                .withProcessors(new GlideAnnotationProcessor())
                .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "import com.bumptech.glide.annotation.GlideOption;",
                    "import com.bumptech.glide.request.RequestOptions;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  @GlideOption",
                    "  public static void centerCrop(RequestOptions options) {}",
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
          public void run() throws Throwable {
            javac()
                .withProcessors(new GlideAnnotationProcessor())
                .compile(
                    emptyAppModule(),
                    JavaFileObjects.forSourceLines(
                        "Extension",
                        "package com.bumptech.glide.test;",
                        "import com.bumptech.glide.annotation.GlideExtension;",
                        "import com.bumptech.glide.annotation.GlideOption;",
                        "import com.bumptech.glide.request.RequestOptions;",
                        "@GlideExtension",
                        "public class Extension {",
                        "  private Extension() {}",
                        "  @GlideOption(override = GlideOption.OVERRIDE_EXTEND)",
                        "  public static void something(RequestOptions options) {}",
                        "}"));
          }
        });
  }

  @Test
  public void compilation_withOverrideExtend_andOverridingMethod_succeeds() {
    Compilation compilation = javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideOption;",
                "import com.bumptech.glide.request.RequestOptions;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @GlideOption(override = GlideOption.OVERRIDE_EXTEND)",
                "  public static void centerCrop(RequestOptions options) {}",
                "}"));
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningContaining("is using a legacy format.");
  }

  @Test
  public void compilation_withOverrideReplace_butNotOverridingMethod_fails() {
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            javac()
                .withProcessors(new GlideAnnotationProcessor())
                .compile(
                    emptyAppModule(),
                    JavaFileObjects.forSourceLines(
                        "Extension",
                        "package com.bumptech.glide.test;",
                        "import com.bumptech.glide.annotation.GlideExtension;",
                        "import com.bumptech.glide.annotation.GlideOption;",
                        "import com.bumptech.glide.request.RequestOptions;",
                        "@GlideExtension",
                        "public class Extension {",
                        "  private Extension() {}",
                        "  @GlideOption(override = GlideOption.OVERRIDE_REPLACE)",
                        "  public static void something(RequestOptions options) {}",
                        "}"));
          }
        });
  }

  @Test
  public void compilation_withOverrideReplace_andOverridingMethod_succeeds() {
    Compilation compilation = javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideOption;",
                "import com.bumptech.glide.request.RequestOptions;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @GlideOption(override = GlideOption.OVERRIDE_REPLACE)",
                "  public static void centerCrop(RequestOptions options) {}",
                "}"));
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningContaining("is using a legacy format.");
  }

  @Test
  public void compilation_withRequestOptionsReturnValue_succeeds() {
     Compilation compilation = javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import android.support.annotation.NonNull;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideOption;",
                "import com.bumptech.glide.request.RequestOptions;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @NonNull",
                "  @GlideOption",
                "  public static RequestOptions doSomething(RequestOptions options) {",
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
                  "import android.support.annotation.NonNull;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "import com.bumptech.glide.annotation.GlideOption;",
                  "import com.bumptech.glide.request.RequestOptions;",
                  "@GlideExtension",
                  "public class WrongReturnTypeExtension {",
                  "  private WrongReturnTypeExtension() {}",
                  "  @NonNull",
                  "  @GlideOption",
                  "  public static Object doSomething(RequestOptions options) {",
                  "    return options;",
                  "  }",
                  "}"));
      fail();
    } catch (RuntimeException e) {
      String message = e.getCause().getMessage();
      Truth.assertThat(message)
          .contains("@GlideOption methods should return a RequestOptions object");
      Truth.assertThat(message).contains("Object");
      Truth.assertThat(message).contains("WrongReturnTypeExtension");
    }
  }

  @Test
  public void compilation_withMissingNonNullAnnotation_warns() {
    Compilation compilation = javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideOption;",
                "import com.bumptech.glide.request.RequestOptions;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @GlideOption",
                "  public static RequestOptions doSomething(RequestOptions options) {",
                "    return options;",
                "  }",
                "}"));
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningCount(1);
    assertThat(compilation).hadWarningContaining("android.support.annotation.NonNull");
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
                  "import android.support.annotation.NonNull;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "import com.bumptech.glide.annotation.GlideOption;",
                  "import com.bumptech.glide.request.RequestOptions;",
                  "@GlideExtension",
                  "public class MissingRequestOptionsExtension {",
                  "  private MissingRequestOptionsExtension() {}",
                  "  @NonNull",
                  "  @GlideOption",
                  "  public static RequestOptions doSomething() {",
                  "    return options;",
                  "  }",
                  "}"));
      fail();
    } catch (RuntimeException e) {
      String message = e.getCause().getMessage();
      Truth.assertThat(message).contains("RequestOptions object as their first parameter");
      Truth.assertThat(message).contains("doSomething");
      Truth.assertThat(message).contains("MissingRequestOptionsExtension");
    }
  }
}
