package com.bumptech.glide.annotation.compiler.test;

import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;

/** Test utilities. */
public final class Util {
  private static final String REGENERATE_TEST_RESOURCES_PROPERTY_NAME =
      "com.bumptech.glide.annotation.compiler.test.regenerate.path";
  private static final String GLIDE_PACKAGE_NAME = "com.bumptech.glide";
  private static final String SUB_PACKAGE_NAME = qualified(GLIDE_PACKAGE_NAME, "test");
  private static final String ANNOTATION_PACKAGE_NAME = "com.bumptech.glide.annotation.compiler";
  private static final String DEFAULT_APP_DIR_NAME = "EmptyAppGlideModuleTest";
  private static final String DEFAULT_LIBRARY_DIR_NAME = "EmptyLibraryGlideModuleTest";
  /**
   * Hardcoded file separator to workaround {@code JavaFileObjects.forResource(...)} defaulting to
   * the unix one.
   */
  private static final String FILE_SEPARATOR = "/";

  private static final String LINE_SEPARATOR = "\n";

  private Util() {
    // Utility class.
  }

  /**
   * Returns the {@code String} from a system property that is expected to contain the project
   * directory for the module containing these tests or {@code null} if we're not currently
   * attempting to regenerate test resources.
   */
  static String getProjectRootIfRegeneratingTestResources() {
    return System.getProperty(REGENERATE_TEST_RESOURCES_PROPERTY_NAME);
  }

  public static JavaFileObject emptyAppModule() {
    return appResource("EmptyAppModule.java");
  }

  public static JavaFileObject emptyLibraryModule() {
    return libraryResource("EmptyLibraryModule.java");
  }

  public static JavaFileObject appResource(String className) {
    return forResource(DEFAULT_APP_DIR_NAME, className);
  }

  public static JavaFileObject libraryResource(String className) {
    return forResource(DEFAULT_LIBRARY_DIR_NAME, className);
  }

  public static JavaFileObject forResource(String directoryName, String name) {
    try {
      return JavaFileObjects.forResource(directoryName + FILE_SEPARATOR + name);
    } catch (IllegalArgumentException e) {
      // IllegalArgumentException will be thrown if the resource is missing. If we're trying to
      // generate test resources for a new test, we want to avoid this exception because it does not
      // contain any expected output that we can write to a file. By returning an empty file, we
      // avoid the exception and get the output from our comparison tests that we can then write
      // out.
      // If we're not regenerating test resources, we should throw the normal exception.
      if (getProjectRootIfRegeneratingTestResources() != null) {
        return JavaFileObjects.forSourceString("com.bumptech.test.empty", "");
      }
      throw e;
    }
  }

  public static String annotation(String className) {
    return qualified(ANNOTATION_PACKAGE_NAME, className);
  }

  public static String subpackage(String className) {
    return qualified(SUB_PACKAGE_NAME, className);
  }

  public static String glide(String className) {
    return qualified(GLIDE_PACKAGE_NAME, className);
  }

  public static CharSequence asUnixChars(CharSequence chars) {
    return chars.toString().replace(System.lineSeparator(), LINE_SEPARATOR);
  }

  private static String qualified(String packageName, String className) {
    return packageName + '.' + className;
  }
}
