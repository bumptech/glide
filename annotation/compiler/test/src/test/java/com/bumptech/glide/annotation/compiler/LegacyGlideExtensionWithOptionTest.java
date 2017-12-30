package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.appResource;
import static com.bumptech.glide.annotation.compiler.test.Util.asUnixChars;
import static com.bumptech.glide.annotation.compiler.test.Util.emptyAppModule;
import static com.bumptech.glide.annotation.compiler.test.Util.glide;
import static com.bumptech.glide.annotation.compiler.test.Util.subpackage;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

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
 * Verifies the output of the processor with a simple single extension option in the legacy
 * option style where extension methods always returned {@code null}.
 */
@RunWith(JUnit4.class)
public class LegacyGlideExtensionWithOptionTest {
  @Rule public final RegenerateResourcesRule regenerateResourcesRule =
      new RegenerateResourcesRule(getClass());
  private Compilation compilation;

  @Before
  public void setUp() {
    compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                forResource("ExtensionWithOption.java"));
    assertThat(compilation).succeeded();
    //noinspection ResultOfMethodCallIgnored
    assertThat(compilation).hadWarningContaining(
        "The squareThumb method annotated with @GlideOption in the ExtensionWithOption"
            + " @GlideExtension is using a legacy format. Support will be removed in a future"
            + " version. Please change your method definition so that your @GlideModule annotated"
            + " methods return RequestOptions objects instead of null.");
  }

  @Test
  public void compilation_generatesAllExpectedFiles() {
    Truth.assertThat(compilation.generatedSourceFiles()).hasSize(7);
  }

  @Test
  public void compilation_generatesExpectedGlideOptionsClass() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(subpackage("GlideOptions"))
        .contentsAsUtf8String()
        .isEqualTo(asUnixChars(forResource("GlideOptions.java").getCharContent(true)));
  }

  @Test
  public void compilation_generatesExpectedGlideRequestClass() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(subpackage("GlideRequest"))
        .contentsAsUtf8String()
        .isEqualTo(asUnixChars(forResource("GlideRequest.java").getCharContent(true)));
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
  @ReferencedResource
  public void compilation_generatesExpectedGeneratedAppGlideModuleImpl() throws IOException {
    assertThat(compilation)
        .generatedSourceFile(glide("GeneratedAppGlideModuleImpl"))
        .contentsAsUtf8String()
        .isEqualTo(
            asUnixChars(appResource("GeneratedAppGlideModuleImpl.java").getCharContent(true)));
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
