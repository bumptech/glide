package com.bumptech.glide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a set of GlideModule and/or LibraryGlideModule classes that should be excluded from an
 * application.
 *
 * <p>Used only on AppGlideModules. Adding this annotation to other classes will have no affect.
 *
 * <p>Cannot be used to exclude AppGlideModules (there must be at most one per Application anyway).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Excludes {
  Class<?>[] value();
}
