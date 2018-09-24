package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.appResource;
import static com.bumptech.glide.annotation.compiler.test.Util.asUnixChars;
import static com.bumptech.glide.annotation.compiler.test.Util.glide;
import static com.bumptech.glide.annotation.compiler.test.Util.subpackage;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

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

/**
 * Tests AppGlideModules that use the @Excludes annotation with multiple excluded Module classes.
 */
@RunWith(JUnit4.class)
public class AppGlideModuleWithMultipleExcludesTest {
  @Rule public final RegenerateResourcesRule regenerateResourcesRule =
      new RegenerateResourcesRule(getClass());
  private Compilation compilation;

  @Before
  public void setUp() {
    compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                forResource("AppModuleWithMultipleExcludes.java"),
                forResource("EmptyLibraryModule1.java"),
                forResource("EmptyLibraryModule2.java"));
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Test
  @ReferencedResource
  public void compilation_generatesExpectedGlideOptionsClass() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(subpackage("GlideOptions"))
        .contentsAsUtf8String()
        .isEqualTo(asUnixChars(appResource("GlideOptions.java").getCharContent(true)));
  }

  @Test
  @ReferencedResource
  public void compilation_generatesExpectedGlideRequestClass() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(subpackage("GlideRequest"))
        .contentsAsUtf8String()
        .isEqualTo(asUnixChars(appResource("GlideRequest.java").getCharContent(true)));
  }

  @Test
  @ReferencedResource
  public void compilation_generatesExpectedGlideRequestsClass() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(subpackage("GlideRequests"))
        .contentsAsUtf8String()
        .isEqualTo(asUnixChars(appResource("GlideRequests.java").getCharContent(true)));
  }

  @Test
  @ReferencedResource
  public void compilationGeneratesExpectedGlideAppClass() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(subpackage("GlideApp"))
        .contentsAsUtf8String()
        .isEqualTo(asUnixChars(appResource("GlideApp.java").getCharContent(true)));
  }

  @Test
  public void compilation_generatesExpectedGeneratedAppGlideModuleImpl() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(glide("GeneratedAppGlideModuleImpl"))
        .contentsAsUtf8String()
        .isEqualTo(
            asUnixChars(forResource("GeneratedAppGlideModuleImpl.java").getCharContent(true)));
  }

  @Test
  @ReferencedResource
  public void compilation_generatesExpectedGeneratedRequestManagerFactory() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(glide("GeneratedRequestManagerFactory"))
        .contentsAsUtf8String()
        .isEqualTo(
            asUnixChars(appResource("GeneratedRequestManagerFactory.java").getCharContent(true)));
  }

  private JavaFileObject forResource(String name) {
    return Util.forResource(getClass().getSimpleName(), name);
  }
}
