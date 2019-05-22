package com.bumptech.glide.annotation.compiler;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.Assert.assertThrows;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests AppGlideModules with invalid usages of the @Excludes annotation. */
// Ignore warnings since most methods use assertThrows
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JUnit4.class)
public class InvalidAppGlideModuleWithExcludesTest {
  @Test
  public void compilation_withMissingExcludedModuleClass_throws() {
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            javac()
                .withProcessors(new GlideAnnotationProcessor())
                .compile(
                    JavaFileObjects.forSourceLines(
                        "AppModuleWithExcludes",
                        "package com.bumptech.glide.test;",
                        "import com.bumptech.glide.annotation.Excludes;",
                        "import com.bumptech.glide.annotation.GlideModule;",
                        "import com.bumptech.glide.module.AppGlideModule;",
                        "import com.bumptech.glide.test.EmptyLibraryModule;",
                        "@GlideModule",
                        "@Excludes(EmptyLibraryModule.class)",
                        "public final class AppModuleWithExcludes extends AppGlideModule {}"));
          }
        });
  }

  @Test
  public void compilation_withEmptyExcludes_fails() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceLines(
                    "AppModuleWithExcludes",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.Excludes;",
                    "import com.bumptech.glide.annotation.GlideModule;",
                    "import com.bumptech.glide.module.AppGlideModule;",
                    "import com.bumptech.glide.test.EmptyLibraryModule;",
                    "@GlideModule",
                    "@Excludes",
                    "public final class AppModuleWithExcludes extends AppGlideModule {}"));
    assertThat(compilation).failed();
  }

  @Test
  public void compilation_withNonGlideModule_throws() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceLines(
                    "AppModuleWithExcludes",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.Excludes;",
                    "import com.bumptech.glide.annotation.GlideModule;",
                    "import com.bumptech.glide.module.AppGlideModule;",
                    "import com.bumptech.glide.test.EmptyLibraryModule;",
                    "@GlideModule",
                    "@Excludes(Object.class)",
                    "public final class AppModuleWithExcludes extends AppGlideModule {}"));
    assertThat(compilation).failed();
  }
}
