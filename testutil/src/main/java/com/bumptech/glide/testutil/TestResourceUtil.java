package com.bumptech.glide.testutil;

import java.io.InputStream;

/** Test only utility for opening resources in androidTest/resources. */
public final class TestResourceUtil {
  private TestResourceUtil() {
    // Utility class
  }

  /**
   * Returns an InputStream for the given test class and sub-path.
   *
   * @param testClass A Junit test class.
   * @param subPath The sub-path under androidTest/resources where the desired resource is located.
   *     Should not be prefixed with a '/'
   */
  public static InputStream openResource(Class<?> testClass, String subPath) {
    return testClass.getResourceAsStream("/" + subPath);
  }
}
