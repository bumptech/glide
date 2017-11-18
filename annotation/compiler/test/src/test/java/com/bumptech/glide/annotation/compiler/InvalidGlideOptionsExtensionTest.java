package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.emptyAppModule;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Checks assertions on {@link com.bumptech.glide.annotation.GlideExtension}s for methods annotated
 * with {@link com.bumptech.glide.annotation.GlideOption}.
 */
// Ignore warnings since most methods use ExpectedException
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JUnit4.class)
public class InvalidGlideOptionsExtensionTest {
  @Rule public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void compilation_withAnnotatedNonStaticMethod_fails() {
    expectedException.expect(RuntimeException.class);
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

  @Test
  public void compilation_withAnnotatedStaticMethod_withoutRequestOptionsArg_fails() {
    expectedException.expect(RuntimeException.class);
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
                "  public static void doSomething() {}",
                "}"));

  }

  @Test
  public void compilation_withAnnotatedStaticMethod_withRequestOptionsArgInWrongOrder_fails() {
    expectedException.expect(RuntimeException.class);
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
                "  public static void doSomething(Object arg1, RequestOptions options) {}",
                "}"));
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
    expectedException.expect(RuntimeException.class);
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

  @Test
  public void compilation_withOverrideExtend_butNotOverridingMethod_fails() {
    expectedException.expect(RuntimeException.class);
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
    expectedException.expect(RuntimeException.class);
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
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Test
  public void compilation_withNonRequestOptionsReturnValue_fails() {
    expectedException.expect(RuntimeException.class);
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
                "  public static Object doSomething(RequestOptions options) {",
                "    return options;",
                "  }",
                "}"));
  }
}
