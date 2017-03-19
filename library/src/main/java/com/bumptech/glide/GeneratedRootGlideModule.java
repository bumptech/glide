package com.bumptech.glide;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bumptech.glide.manager.RequestManagerRetriever;
import com.bumptech.glide.module.RootGlideModule;
import java.util.Set;

/**
 * A temporary interface to allow {@link RootGlideModule}s to exclude
 * {@link com.bumptech.glide.annotation.GlideModule}s to ease the migration from
 * {@link com.bumptech.glide.annotation.GlideModule}s to Glide's annotation processing system.
 */
@Deprecated
abstract class GeneratedRootGlideModule extends RootGlideModule {
  /**
   * This method can be removed when manifest parsing is no longer supported.
   */
  @Deprecated
  @NonNull
  abstract Set<Class<?>> getExcludedModuleClasses();

  @Nullable
  RequestManagerRetriever.RequestManagerFactory getRequestManagerFactory() {
    return null;
  }
}
