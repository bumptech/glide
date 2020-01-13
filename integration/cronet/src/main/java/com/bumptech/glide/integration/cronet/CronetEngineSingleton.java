package com.bumptech.glide.integration.cronet;

import android.content.Context;
import org.chromium.net.CronetEngine;

/**
 * Class controlling singleton instance of the CronetEngine. Ensures at most one instance of the
 * CronetEngine is created.
 */
// NOTE: This is a standalone class and not a memoized supplier as the CronetEngine creations
// requires a parameter, namedly a Context reference.
public final class CronetEngineSingleton {

  // non instantiable
  private CronetEngineSingleton() {}

  private static volatile CronetEngine cronetEngineSingleton;

  public static CronetEngine getSingleton(Context context) {

    // Lazily create the engine.
    if (cronetEngineSingleton == null) {
      synchronized (CronetEngineSingleton.class) {
        // have to re-check since this might have changed before synchronization, but we don't
        // want to synchronize just to check for null.
        if (cronetEngineSingleton == null) {
          cronetEngineSingleton = createEngine(context);
        }
      }
    }

    return cronetEngineSingleton;
  }

  private static CronetEngine createEngine(Context context) {
    return new CronetEngine.Builder(context)
        .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISABLED, 0)
        .enableHttp2(true)
        .enableQuic(false)
        .build();
  }
}
