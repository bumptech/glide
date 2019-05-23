package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.annotation;
import static com.bumptech.glide.annotation.compiler.test.Util.appResource;
import static com.bumptech.glide.annotation.compiler.test.Util.emptyAppModule;
import static com.bumptech.glide.annotation.compiler.test.Util.emptyLibraryModule;
import static com.bumptech.glide.annotation.compiler.test.Util.glide;
import static com.bumptech.glide.annotation.compiler.test.Util.libraryResource;
import static com.bumptech.glide.annotation.compiler.test.Util.subpackage;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.bumptech.glide.annotation.compiler.test.CompilationProvider;
import com.bumptech.glide.annotation.compiler.test.ReferencedResource;
import com.bumptech.glide.annotation.compiler.test.RegenerateResourcesRule;
import com.bumptech.glide.annotation.compiler.test.Util;
import com.google.common.truth.Truth;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import javax.tools.JavaFileObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests adding both an empty {@link com.bumptech.glide.module.AppGlideModule} and an empty {@link
 * com.bumptech.glide.module.LibraryGlideModule} in a single project.
 */
@RunWith(JUnit4.class)
public class EmptyAppAndLibraryGlideModulesTest implements CompilationProvider {
  @Rule
  public final RegenerateResourcesRule regenerateResourcesRule = new RegenerateResourcesRule(this);

  private Compilation compilation;

  @Before
  public void setUp() {
    compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(emptyAppModule(), emptyLibraryModule());
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Test
  public void compilation_generatesAllExpectedFiles() {
    Truth.assertThat(compilation.generatedSourceFiles()).hasSize(7);
  }

  @Test
  @ReferencedResource
  public void compilation_generatesExpectedGlideOptionsClass() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(subpackage("GlideOptions"))
        .hasSourceEquivalentTo(appResource("GlideOptions.java"));
  }

  @Test
  @ReferencedResource
  public void compilation_generatesExpectedGlideRequestClass() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(subpackage("GlideRequest"))
        .hasSourceEquivalentTo(appResource("GlideRequest.java"));
  }

  @Test
  @ReferencedResource
  public void compilation_generatesExpectedGlideRequestsClass() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(subpackage("GlideRequests"))
        .hasSourceEquivalentTo(appResource("GlideRequests.java"));
  }

  @Test
  @ReferencedResource
  public void compilationGeneratesExpectedGlideAppClass() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(subpackage("GlideApp"))
        .hasSourceEquivalentTo(appResource("GlideApp.java"));
  }

  @Test
  public void compilation_generatesExpectedGeneratedAppGlideModuleImpl() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(glide("GeneratedAppGlideModuleImpl"))
        .hasSourceEquivalentTo(forResource("GeneratedAppGlideModuleImpl.java"));
  }

  @Test
  @ReferencedResource
  public void compilation_generatesExpectedGeneratedRequestManagerFactory() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(glide("GeneratedRequestManagerFactory"))
        .hasSourceEquivalentTo(appResource("GeneratedRequestManagerFactory.java"));
  }

  @Test
  @ReferencedResource
  public void compilation_generatesExpectedIndexer() throws IOException {
    String expectedClassName =
        "GlideIndexer_GlideModule_com_bumptech_glide_test_EmptyLibraryModule";
    assertThat(compilation)
        .generatedSourceFile(annotation(expectedClassName))
        .hasSourceEquivalentTo(libraryResource(expectedClassName + ".java"));
  }

  private JavaFileObject forResource(String name) {
    return Util.forResource(getClass().getSimpleName(), name);
  }

  @Override
  public Compilation getCompilation() {
    return compilation;
  }
}
