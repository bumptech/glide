package com.bumptech.glide.annotation.compiler.test;

import static com.bumptech.glide.annotation.compiler.test.Util.asUnixChars;

import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import javax.tools.JavaFileObject;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Regenerates test resources for annotation compiler tests when the {@link
 * Util#REGENERATE_TEST_RESOURCES_PROPERTY_NAME} property is set to the directory containing the
 * project.
 *
 * <p>This can easily be used via gradle by running: {@code ./gradlew
 * :annotation:compiler:test:regenerateTestResources }
 *
 * <p>Our regenerate task will set the appropriate environment variables that will allow the logic
 * here to succeed. When running the tests normally, this class will do nothing.
 */
public final class RegenerateResourcesRule implements TestRule {

  private CompilationProvider test;

  public RegenerateResourcesRule(CompilationProvider test) {
    this.test = test;
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } catch (AssertionError e) {
          String projectRoot = Util.getProjectRootIfRegeneratingTestResources();
          if (projectRoot == null || description.getAnnotation(ReferencedResource.class) != null) {
            throw e;
          }
          updateResourceFile(e, projectRoot, description);
        }
      }
    };
  }

  private void updateResourceFile(
      AssertionError e, @NonNull String projectDirectory, Description description) {
    String testClassName = test.getClass().getSimpleName();
    String testFileName = parseFileNameFromMessage(e);
    String testDirectory = projectDirectory + "/src/test/resources/" + testClassName;
    String subDirectorySegment =
        description.getAnnotation(SubDirectory.class) != null
            ? description.getAnnotation(SubDirectory.class).value() + "/"
            : "";

    File expectedDirectory = new File(testDirectory + "/" + subDirectorySegment);
    if (!expectedDirectory.exists() && !expectedDirectory.mkdirs()) {
      throw new IllegalStateException(
          "Failed to generate expected directory: " + expectedDirectory);
    }
    if (!expectedDirectory.isDirectory()) {
      throw new IllegalStateException(
          "Expected a directory, but found a file: " + expectedDirectory);
    }

    File expectedFile = new File(expectedDirectory, testFileName);
    Writer writer = null;
    try {
      writer = new FileWriter(expectedFile);
      writer.write(asUnixChars(parseActual(testFileName)).toString());
      writer.close();
    } catch (IOException e1) {
      throw new RuntimeException("Failed to regenerate test file", e1);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException exception) {
          // Ignore.
        }
      }
    }
  }

  private String parseActual(String fileName) {
    for (JavaFileObject javaFileObject : test.getCompilation().generatedSourceFiles()) {
      if (javaFileObject.getName().contains(fileName)) {
        try {
          return javaFileObject.getCharContent(true).toString();
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
    }
    throw new IllegalStateException("Failed to find source file for name: " + fileName);
  }

  // Parses </SOURCE_OUTPUT/com/bumptech/glide/test/GlideOptions.java> to GlideOptions.java.
  private static String parseFileNameFromMessage(AssertionError e) {
    String message = e.getMessage();
    int firstGreaterThanIndex = message.indexOf('>');
    String substring = message.substring(0, firstGreaterThanIndex);
    int lastForwardSlashIndex = substring.lastIndexOf('/');
    return substring.substring(lastForwardSlashIndex + 1, substring.length());
  }
}
