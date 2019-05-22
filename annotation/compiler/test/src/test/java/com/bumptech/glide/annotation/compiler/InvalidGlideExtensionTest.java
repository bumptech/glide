package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.emptyAppModule;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.Assert.fail;

import com.google.common.truth.Truth;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Checks assertions on {@link com.bumptech.glide.annotation.GlideExtension}s themselves. */
// Avoid warnings when asserting on exceptions.
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JUnit4.class)
public class InvalidGlideExtensionTest {
  @Test
  public void compilation_withPublicConstructor_fails() {
    try {
      javac()
          .withProcessors(new GlideAnnotationProcessor())
          .compile(
              emptyAppModule(),
              JavaFileObjects.forSourceLines(
                  "PublicConstructor",
                  "package com.bumptech.glide.test;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "@GlideExtension",
                  "public class PublicConstructor { }"));
      fail("Failed to throw expected exception");
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      Truth.assertThat(cause.getMessage()).contains("non-private constructor");
      Truth.assertThat(cause.getMessage()).contains("PublicConstructor");
    }
  }

  @Test
  public void compilation_withPackagePrivateExtension_fails() {
    try {
      javac()
          .withProcessors(new GlideAnnotationProcessor())
          .compile(
              emptyAppModule(),
              JavaFileObjects.forSourceLines(
                  "PackagePrivateExtension",
                  "package com.bumptech.glide.test;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "@GlideExtension",
                  "class PackagePrivateExtension {",
                  "  private PackagePrivateExtension() {}",
                  "}"));
      fail("Failed to throw expected exception");
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      Truth.assertThat(cause.getMessage()).contains("must be public");
      Truth.assertThat(cause.getMessage()).contains("PackagePrivateExtension");
    }
  }

  @Test
  public void compilation_withConstructorWithParameters_throws() {
    try {
      javac()
          .withProcessors(new GlideAnnotationProcessor())
          .compile(
              emptyAppModule(),
              JavaFileObjects.forSourceLines(
                  "ConstructorParametersExtension",
                  "package com.bumptech.glide.test;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "@GlideExtension",
                  "public class ConstructorParametersExtension {",
                  "  private ConstructorParametersExtension(int failParam) {}",
                  "  public void doSomething() {}",
                  "}"));
      fail("Failed to get expected exception");
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      Truth.assertThat(cause.getMessage()).contains("parameters in the constructor");
      Truth.assertThat(cause.getMessage()).contains("ConstructorParametersExtension");
    }
  }

  @Test
  public void compilation_withNonStaticMethod_succeeds() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  public void doSomething() {}",
                    "}"));
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Test
  public void compilation_withStaticMethod_succeeds() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  public static void doSomething() {}",
                    "}"));
    assertThat(compilation).succeededWithoutWarnings();
  }
}
