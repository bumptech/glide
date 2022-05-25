package com.bumptech.glide.annotation.compiler;

import static com.google.testing.compile.Compiler.javac;

import com.bumptech.glide.annotation.compiler.test.CompilationProvider;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.JavaFileObjects;
import java.io.File;
import java.io.IOException;
import javax.tools.JavaFileObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Makes sure that we can handle indexers based on long package or file names, or many modules.
 *
 * <p>See #4106.
 */
@RunWith(JUnit4.class)
public class OverlyLongFileNameTest implements CompilationProvider {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private Compilation compilation;
  private static final String FILE_NAME_LONGER_THAN_255_CHARS =
      "SomeReallyReallyRidiculouslyLongFileNameOrPackageNameIGuessThatExceedsTwoHundredAndFiftyFive"
          + "CharactersThoughThatsOnlyAroundOneHundredCharactersWhichMeansINeedToKeepTypingToGetTo"
          + "TwoHundredAndFiftyFiveSomehowThankfullyOnlyLikeFiftyToGoNowMaybeButNotQuiteYet"
          + "SomewhereAroundNowIsProbablyGood";

  @Before
  public void setUp() {
    compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceLines(
                    FILE_NAME_LONGER_THAN_255_CHARS,
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.annotation.GlideModule;",
                    "import com.bumptech.glide.module.LibraryGlideModule;",
                    "@GlideModule",
                    "public final class "
                        + FILE_NAME_LONGER_THAN_255_CHARS
                        + " extends LibraryGlideModule {}"));
  }

  @Test
  public void compilingLongClassAndOrPackageNameShouldSucceed() throws IOException {
    CompilationSubject.assertThat(compilation).succeededWithoutWarnings();
    for (JavaFileObject file : compilation.generatedFiles()) {
      temporaryFolder.create();
      String actualFileName = new File(file.getName()).getName();
      if (!actualFileName.startsWith(FILE_NAME_LONGER_THAN_255_CHARS)) {
        try {
          temporaryFolder.newFile(actualFileName).createNewFile();
        } catch (IOException e) {
          throw new RuntimeException("Failed to create: " + actualFileName, e);
        }
      }
    }
  }

  @Override
  public Compilation getCompilation() {
    return compilation;
  }
}
