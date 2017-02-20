package com.bumptech.glide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a set of GlideModule and/or ChildGlideModule classes that should be excluded
 * from an application.
 *
 * <p>Used only on RootGlideModules. Adding this annotation to other classes will have no affect.
 *
 * <p>Cannot be used to exclude RootGlideModules (there must be at most one per Application anyway).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Excludes {
  Class[] value();
}
