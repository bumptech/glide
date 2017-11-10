package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.emptyAppModule;
import static com.bumptech.glide.annotation.compiler.test.Util.subpackage;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.bumptech.glide.annotation.compiler.test.Util;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Verifies only the output we expect to change based on the various configurations of GlideOptions.
 *
 * <p>The output for all classes is tested in {@link LegacyGlideExtensionWithOptionTest}.
 */
@RunWith(JUnit4.class)
public class GlideExtensionOptionsTest {
  private static final String EXTENSION_NAME = "Extension.java";

  @Test
  public void compilation_withOverrideExtend_validOptions() throws IOException {
    runTest("OverrideExtend", Subject.GlideOptions);
  }

  @Test
  public void compilation_withOverrideExtend_validRequest() throws IOException {
    runTest("OverrideExtend", Subject.GlideRequest);
  }

  @Test
  public void compilation_withOverrideReplace_andMultipleArguments_validOptions()
      throws IOException {
    runTest("OverrideExtendMultipleArguments", Subject.GlideOptions);
  }

  @Test
  public void compilation_withOverrideReplace_andMultipleArguments_validRequest()
      throws IOException {
    runTest("OverrideExtendMultipleArguments", Subject.GlideRequest);
  }

  @Test
  public void compilation_withOverrideReplace_validOptions() throws IOException {
    runTest("OverrideReplace", Subject.GlideOptions);
  }

  @Test
  public void compilation_withOverrideReplace_validRequest() throws IOException {
    runTest("OverrideReplace", Subject.GlideRequest);
  }

  @Test
  public void compilation_withStaticMethodName_validOptions() throws IOException {
    runTest("StaticMethodName", Subject.GlideOptions);
  }

  @Test
  public void compilation_withStaticMethodName_validRequest() throws IOException {
    runTest("StaticMethodName", Subject.GlideRequest);
  }

  @Test
  public void compilation_withMemoizeStaticMethod_validOptions() throws IOException {
    runTest("MemoizeStaticMethod", Subject.GlideOptions);
  }

  @Test
  public void compilation_withMemoizeStaticMethod_validRequest() throws IOException {
    runTest("MemoizeStaticMethod", Subject.GlideRequest);
  }

  @Test
  public void compilation_withSkipStaticMethod_validOptions() throws IOException {
    runTest("SkipStaticMethod", Subject.GlideOptions);
  }

  @Test
  public void compilation_withSkipStaticMethod_validRequest() throws IOException {
    runTest("SkipStaticMethod", Subject.GlideRequest);
  }

  private enum Subject {
    GlideOptions,
    GlideRequest;

    String file() {
      return name() + ".java";
    }
  }

  private void runTest(String subDir, Subject subject) throws IOException {
     Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                extension(subDir));
    assertThat(compilation).succeededWithoutWarnings();

    assertThat(compilation)
        .generatedSourceFile(subpackage(subject.name()))
        .contentsAsUtf8String()
        .isEqualTo(forResource(subDir, subject.file()).getCharContent(true));
  }

  private JavaFileObject extension(String subdir) {
    return forResource(subdir, EXTENSION_NAME);
  }

  private JavaFileObject forResource(String subdir, String name) {
    return Util.forResource(getClass().getSimpleName(), subdir + "/" + name);
  }
}
