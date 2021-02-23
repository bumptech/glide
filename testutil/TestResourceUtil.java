package com.bumptech.glide.testutil;

import com.google.testing.util.TestUtil;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * A shim test class to enable us to retrieve resources in Google3 and in the gradle build from
 * Github.This class should never be exported into Github and there is a matching class in Github
 * that should never be imported into Google.
 */
public final class TestResourceUtil {

  private static final String GLIDE_PATH = "/google3/third_party/java_src/android_libs/glide/";
  private static final String[] RESOURCE_SUB_PATHS =
      new String[] {
        "third_party/exif_orientation_examples/",
        "library/test/src/test/resources/",
        "third_party/gif_decoder/src/test/resources/"
      };

  private TestResourceUtil() {
    // Utility class.
  }

  public static InputStream openResource(Class testClass, String subPath) {
    InputStream result = null;
    for (String resourcePath : RESOURCE_SUB_PATHS) {
      try {
        result =
            new FileInputStream(TestUtil.getRunfilesDir() + GLIDE_PATH + resourcePath + subPath);
        break;
      } catch (FileNotFoundException e) {
        // Ignore.
      }
    }
    if (result == null) {
      throw new IllegalArgumentException("Unable to find resource: " + subPath);
    }
    return result;
  }
}
