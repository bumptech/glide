package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.asUnixChars;
import static com.bumptech.glide.annotation.compiler.test.Util.emptyAppModule;
import static com.bumptech.glide.annotation.compiler.test.Util.subpackage;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.bumptech.glide.annotation.compiler.test.RegenerateResourcesRule;
import com.bumptech.glide.annotation.compiler.test.SubDirectory;
import com.bumptech.glide.annotation.compiler.test.TestDescription;
import com.bumptech.glide.annotation.compiler.test.Util;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import javax.tools.JavaFileObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Verifies only the output we expect to change based on the various configurations of GlideOptions
 * when GlideOptions are defined in the legacy format.
 *
 * <p>The output for all classes is tested in {@link LegacyGlideExtensionWithOptionTest}.
 */
@RunWith(JUnit4.class)
public class LegacyGlideExtensionOptionsTest {
  @Rule public final TestDescription testDescription = new TestDescription();
  @Rule public final RegenerateResourcesRule regenerateResourcesRule =
      new RegenerateResourcesRule(getClass());

  private static final String EXTENSION_NAME = "Extension.java";

  @Test
  @SubDirectory("OverrideExtend")
  public void compilation_withOverrideExtend_validOptions() throws IOException {
    runTest(Subject.GlideOptions);
  }

  @Test
  @SubDirectory("OverrideExtend")
  public void compilation_withOverrideExtend_validRequest() throws IOException {
    runTest(Subject.GlideRequest);
  }

  @Test
  @SubDirectory("OverrideReplace")
  public void compilation_withOverrideReplace_validOptions() throws IOException {
    runTest(Subject.GlideOptions);
  }

  @Test
  @SubDirectory("OverrideReplace")
  public void compilation_withOverrideReplace_validRequest() throws IOException {
    runTest(Subject.GlideRequest);
  }

  @Test
  @SubDirectory("StaticMethodName")
  public void compilation_withStaticMethodName_validOptions() throws IOException {
    runTest(Subject.GlideOptions);
  }

  @Test
  @SubDirectory("StaticMethodName")
  public void compilation_withStaticMethodName_validRequest() throws IOException {
    runTest(Subject.GlideRequest);
  }

  @Test
  @SubDirectory("MemoizeStaticMethod")
  public void compilation_withMemoizeStaticMethod_validOptions() throws IOException {
    runTest(Subject.GlideOptions);
  }

  @Test
  @SubDirectory("MemoizeStaticMethod")
  public void compilation_withMemoizeStaticMethod_validRequest() throws IOException {
    runTest(Subject.GlideRequest);
  }

  @Test
  @SubDirectory("SkipStaticMethod")
  public void compilation_withSkipStaticMethod_validOptions() throws IOException {
    runTest(Subject.GlideOptions);
  }

  @Test
  @SubDirectory("SkipStaticMethod")
  public void compilation_withSkipStaticMethod_validRequest() throws IOException {
    runTest(Subject.GlideRequest);
  }

  private enum Subject {
    GlideOptions,
    GlideRequest;

    String file() {
      return name() + ".java";
    }
  }

  private void runTest(Subject subject) throws IOException {
    String subDirectoryName = getSubDirectoryName();
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                extension(subDirectoryName));
    assertThat(compilation).succeeded();

    assertThat(compilation)
        .generatedSourceFile(subpackage(subject.name()))
        .contentsAsUtf8String()
        .isEqualTo(asUnixChars(forResource(subDirectoryName, subject.file()).getCharContent(true)));
  }

  private String getSubDirectoryName() {
    return testDescription
        .getDescription()
        .getAnnotation(SubDirectory.class)
        .value();
  }

  private JavaFileObject extension(String subdir) {
    return forResource(subdir, EXTENSION_NAME);
  }

  private JavaFileObject forResource(String subdir, String name) {
    return Util.forResource(getClass().getSimpleName(), subdir + "/" + name);
  }
}
