package android.support.test;

import android.content.Context;

// Workaround for https://github.com/mockito/mockito/issues/1472.
public final class InstrumentationRegistry {
  public static Context getTargetContext() {
    return androidx.test.InstrumentationRegistry.getTargetContext();
  }
}
