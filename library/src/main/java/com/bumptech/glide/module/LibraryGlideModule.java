package com.bumptech.glide.module;

import android.content.Context;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;

/**
 * Registers a set of components to use when initializing Glide within an app when
 * Glide's annotation processor is used.
 *
 * <p>Any number of LibraryGlideModules can be contained within any library or application.
 *
 * <p>LibraryGlideModules are called in no defined order. If LibraryGlideModules within an
 * application conflict, {@link AppGlideModule}s can use the
 * {@link com.bumptech.glide.annotation.Excludes} annotation to selectively remove one or more of
 * the conflicting modules.
 */
public abstract class LibraryGlideModule implements RegistersComponents {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    // Default empty impl.
  }
}
