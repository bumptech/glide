package com.bumptech.glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.manager.RequestManagerRetriever;
import com.bumptech.glide.module.AppGlideModule;
import java.util.HashSet;
import java.util.Set;

/**
 * Allows {@link AppGlideModule}s to exclude {@link com.bumptech.glide.annotation.GlideModule}s to
 * ease the migration from {@link com.bumptech.glide.annotation.GlideModule}s to Glide's annotation
 * processing system and optionally provides a {@link
 * com.bumptech.glide.manager.RequestManagerRetriever.RequestManagerFactory} impl.
 */
abstract class GeneratedAppGlideModule extends AppGlideModule {
  /** This method can be removed when manifest parsing is no longer supported. */
  @NonNull
  Set<Class<?>> getExcludedModuleClasses() {
    return new HashSet<>();
  }

  @Nullable
  RequestManagerRetriever.RequestManagerFactory getRequestManagerFactory() {
    return null;
  }
}
