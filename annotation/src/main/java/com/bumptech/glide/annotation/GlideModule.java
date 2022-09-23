package com.bumptech.glide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies AppGlideModules and LibraryGlideModules for Glide's annotation processor to merge at
 * compile time.
 *
 * <p>Replaces {@code <meta-data />} tags in AndroidManifest.xml.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface GlideModule {
  /**
   * Returns the name of the class that will be used as a replacement for {@code
   * com.bumptech.glide.Glide} in Applications that depend on Glide's generated code.
   */
  String glideName() default "GlideApp";
}
