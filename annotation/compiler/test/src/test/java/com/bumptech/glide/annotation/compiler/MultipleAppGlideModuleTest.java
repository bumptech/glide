package com.bumptech.glide.annotation.compiler;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.bumptech.glide.annotation.compiler.test.Util;
import com.google.testing.compile.Compilation;
import javax.tools.JavaFileObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Ensures that adding more than one {@link com.bumptech.glide.module.AppGlideModule} to a project
 * will fail.
 */
public class MultipleAppGlideModuleTest {
  @Rule public ExpectedException expectedException = ExpectedException.none();
  private static final String DIR_NAME = MultipleAppGlideModuleTest.class.getSimpleName();
  private static final String FIRST_MODULE = "EmptyAppModule1.java";
  private static final String SECOND_MODULE = "EmptyAppModule2.java";

  // Throws.
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void compilation_withTwoAppModules_fails() {
    expectedException.expect(RuntimeException.class);
    javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(forResource(FIRST_MODULE), forResource(SECOND_MODULE));
  }

  @Test
  public void compilation_withFirstModuleOnly_succeeds() {
    Compilation compilation = javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(forResource(FIRST_MODULE));
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Test
  public void compilation_withSecondModuleOnly_succeeds() {
    Compilation compilation = javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(forResource(SECOND_MODULE));
    assertThat(compilation).succeededWithoutWarnings();
  }

  private static JavaFileObject forResource(String name) {
    return Util.forResource(DIR_NAME, name);
  }
}
