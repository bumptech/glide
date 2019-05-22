package com.bumptech.glide.annotation.compiler.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the subdirectory for a particular test that contains the test resource(s) used for the
 * method.
 *
 * <p>Used both by tests to extract the correct subdirectory and by the {@link
 * RegenerateResourcesRule} for the same purpose.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubDirectory {
  String value();
}
