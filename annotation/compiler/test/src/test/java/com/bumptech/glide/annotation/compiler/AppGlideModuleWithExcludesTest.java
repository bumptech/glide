package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.appResource;
import static com.bumptech.glide.annotation.compiler.test.Util.emptyLibraryModule;
import static com.bumptech.glide.annotation.compiler.test.Util.glide;
import static com.bumptech.glide.annotation.compiler.test.Util.subpackage;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.bumptech.glide.annotation.compiler.test.CompilationProvider;
import com.bumptech.glide.annotation.compiler.test.ReferencedResource;
import com.bumptech.glide.annotation.compiler.test.RegenerateResourcesRule;
import com.bumptech.glide.annotation.compiler.test.Util;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import javax.tools.JavaFileObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests AppGlideModules that use the @Excludes annotation with a single excluded Module class. */
@RunWith(JUnit4.class)
public class AppGlideModuleWithExcludesTest implements CompilationProvider {
  @Rule
  public final RegenerateResourcesRule regenerateResourcesRule = new RegenerateResourcesRule(this);

  private Compilation compilation;

  @Before
  public void setUp() {
    compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(forResource("AppModuleWithExcludes.java"), emptyLibraryModule());
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Override
  public Compilation getCompilation() {
    return compilation;
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

  private JavaFileObject forResource(String name) {
    return Util.forResource(getClass().getSimpleName(), name);
  }
}
