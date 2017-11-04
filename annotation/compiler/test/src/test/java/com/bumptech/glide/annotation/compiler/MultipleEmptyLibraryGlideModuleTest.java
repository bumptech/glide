package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.annotation;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.bumptech.glide.annotation.compiler.test.Util;
import com.google.common.truth.Truth;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import javax.tools.JavaFileObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests adding multiple {@link com.bumptech.glide.module.LibraryGlideModule}s in a project.
 */
public class MultipleEmptyLibraryGlideModuleTest {
  private static final String DIR_NAME = "MultipleEmptyLibraryGlideModuleTest";
  private Compilation compilation;

  @Before
  public void setUp() {
    compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                forResource("EmptyLibraryModule1.java"),
                forResource("EmptyLibraryModule2.java"));
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Test
  public void compilation_generatesAllExpectedFiles() {
    Truth.assertThat(compilation.generatedSourceFiles()).hasSize(1);
  }

  @Test
  public void compilation_generatesExpectedIndexerForModules() throws IOException {
    String expectedClassName =
        "GlideIndexer_GlideModule_com_bumptech_glide_test_EmptyLibraryModule1_com_bumptech_glide"
            + "_test_EmptyLibraryModule2";
    assertThat(compilation)
        .generatedSourceFile(annotation(expectedClassName))
        .contentsAsUtf8String()
        .isEqualTo(forResource(expectedClassName + ".java").getCharContent(true));
  }

  private static JavaFileObject forResource(String name) {
    return Util.forResource(DIR_NAME, name);
  }
}
