package com.bumptech.glide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies RootGlideModules and ChildeGlideModules for Glide's annotation processor to merge at
 * compile time.
 *
 * <p>Replaces <meta-data /> tags in AndroidManifest.xml.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GlideModule { }
