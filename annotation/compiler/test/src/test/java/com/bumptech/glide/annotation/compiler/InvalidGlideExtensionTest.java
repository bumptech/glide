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
 * Checks assertions on {@link com.bumptech.glide.annotation.GlideExtension}s themselves.
 */
// Avoid warnings when using ExpectedException.
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JUnit4.class)
public class InvalidGlideExtensionTest {
  @Rule public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void compilation_withPublicConstructor_fails() {
    expectedException.expect(RuntimeException.class);
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
  }

  @Test
  public void compilation_withExtension_fails() {
    expectedException.expect(RuntimeException.class);
    javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "@GlideExtension",
                "class Extension {",
                "  private Extension() {}",
                "}"));
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
