package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.test.Util.emptyAppModule;
import static com.bumptech.glide.annotation.compiler.test.Util.subpackage;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.bumptech.glide.annotation.compiler.test.CompilationProvider;
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
 * Verifies only the output we expect to change based on the various configurations of GlideOptions.
 */
@RunWith(JUnit4.class)
public class GlideExtensionOptionsTest implements CompilationProvider {
  @Rule
  public final RegenerateResourcesRule regenerateResourcesRule = new RegenerateResourcesRule(this);

  @Rule public final TestDescription testDescription = new TestDescription();
  private static final String EXTENSION_NAME = "Extension.java";
  private Compilation currentCompilation;

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
  @SubDirectory("OverrideExtendMultipleArguments")
  public void compilation_withOverrideReplace_andMultipleArguments_validOptions()
      throws IOException {
    runTest(Subject.GlideOptions);
  }

  @Test
  @SubDirectory("OverrideExtendMultipleArguments")
  public void compilation_withOverrideReplace_andMultipleArguments_validRequest()
      throws IOException {
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

  @Override
  public Compilation getCompilation() {
    return currentCompilation;
  }

  private enum Subject {
    GlideOptions,
    GlideRequest;

    String file() {
      return name() + ".java";
    }
  }

  private void runTest(Subject subject) {
    String subDir = getSubDirectoryName();
    currentCompilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(emptyAppModule(), extension(subDir));
    assertThat(currentCompilation).succeededWithoutWarnings();

    assertThat(currentCompilation)
        .generatedSourceFile(subpackage(subject.name()))
        .hasSourceEquivalentTo(forResource(subDir, subject.file()));
  }

  private String getSubDirectoryName() {
    return testDescription.getDescription().getAnnotation(SubDirectory.class).value();
  }

  private JavaFileObject extension(String subdir) {
    return forResource(subdir, EXTENSION_NAME);
  }

  private JavaFileObject forResource(String subdir, String name) {
    return Util.forResource(getClass().getSimpleName(), subdir + "/" + name);
  }
}
