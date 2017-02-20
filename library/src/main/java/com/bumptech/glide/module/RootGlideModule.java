package com.bumptech.glide.module;

import android.content.Context;
import com.bumptech.glide.GlideBuilder;

/**
 * Defines a set of dependencies and options to use when initializing Glide within an application.
 *
 * <p>There can be at most one RootGlideModule in an application. Only Applications can include a
 * RootGlideModule. Libraries must use {@link ChildGlideModule}.
 *
 * <p>Classes that extend RootGlideModule must be annotated with
 * {@link com.bumptech.glide.annotation.GlideModule} to be processed correctly.
 *
 * <p>Classes that extend RootGlideModule can optionally be annotated with
 * {@link com.bumptech.glide.annotation.Excludes} to optionally exclude one or more
 * {@link ChildGlideModule} and/or {@link GlideModule} classes.
 *
 * <p>Once an application has migrated itself and all libraries it depends on to use Glide's
 * annotation processor, RootGlideModule implementations should override
 * {@link #isManifestParsingEnabled()} and return {@code false}.
 */
public abstract class RootGlideModule extends ChildGlideModule implements AppliesOptions {
  /**
   * Returns {@code true} if Glide should check the AndroidManifest for {@link GlideModule}s.
   *
   * <p>Implementations should return {@code false} after they and their dependencies have migrated
   * to Glide's annotation processor.
   *
   * <p>Returns {@code true} by default.
   */
  public boolean isManifestParsingEnabled() {
    return true;
  }

  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    // Default empty impl.
  }
}
