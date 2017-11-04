package com.bumptech.glide.annotation.compiler.test;

import com.google.testing.compile.JavaFileObjects;
import java.io.File;
import javax.tools.JavaFileObject;

/** Test utilities. */
public final class Util {
  private static final String GLIDE_PACKAGE_NAME = "com.bumptech.glide";
  private static final String SUB_PACKAGE_NAME = qualified(GLIDE_PACKAGE_NAME, "test");
  private static final String ANNOTATION_PACKAGE_NAME = "com.bumptech.glide.annotation.compiler";
  private static final String DEFAULT_APP_DIR_NAME = "EmptyAppGlideModuleTest";
  private static final String DEFAULT_LIBRARY_DIR_NAME = "EmptyLibraryGlideModuleTest";

  private Util() {
    // Utility class.
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
    return JavaFileObjects.forResource(directoryName + File.separator + name);
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

  private static String qualified(String packageName, String className) {
    return packageName + '.' + className;
  }
}
