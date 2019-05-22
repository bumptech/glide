package com.bumptech.glide.annotation.compiler;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.Assert.assertThrows;

import com.bumptech.glide.annotation.compiler.test.CompilationProvider;
import com.bumptech.glide.annotation.compiler.test.RegenerateResourcesRule;
import com.bumptech.glide.annotation.compiler.test.Util;
import com.google.testing.compile.Compilation;
import javax.tools.JavaFileObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Ensures that adding more than one {@link com.bumptech.glide.module.AppGlideModule} to a project
 * will fail.
 */
@RunWith(JUnit4.class)
public class MultipleAppGlideModuleTest implements CompilationProvider {
  private static final String FIRST_MODULE = "EmptyAppModule1.java";
  private static final String SECOND_MODULE = "EmptyAppModule2.java";

  @Rule
  public final RegenerateResourcesRule regenerateResourcesRule = new RegenerateResourcesRule(this);

  private Compilation compilation;

  // Throws.
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void compilation_withTwoAppModules_fails() {
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            javac()
                .withProcessors(new GlideAnnotationProcessor())
                .compile(forResource(FIRST_MODULE), forResource(SECOND_MODULE));
          }
        });
  }

  @Test
  public void compilation_withFirstModuleOnly_succeeds() {
    compilation =
        javac().withProcessors(new GlideAnnotationProcessor()).compile(forResource(FIRST_MODULE));
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Test
  public void compilation_withSecondModuleOnly_succeeds() {
    compilation =
        javac().withProcessors(new GlideAnnotationProcessor()).compile(forResource(SECOND_MODULE));
    assertThat(compilation).succeededWithoutWarnings();
  }

  private JavaFileObject forResource(String name) {
    return Util.forResource(getClass().getSimpleName(), name);
  }

  @Override
  public Compilation getCompilation() {
    return compilation;
  }
}
