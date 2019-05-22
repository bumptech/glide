package com.bumptech.glide.tests;

import android.util.Log;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLog;

/**
 * Exists only to "enable" logging for test coverage.
 *
 * <p>TODO: when we can ignore Log.* via configuration, remove this class.
 */
@Implements(Log.class)
public class GlideShadowLog extends ShadowLog {

  @Implementation
  public static boolean isLoggable(String tag, int level) {
    return true;
  }
}
