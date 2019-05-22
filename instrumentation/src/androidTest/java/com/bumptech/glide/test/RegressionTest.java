package com.bumptech.glide.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a test is a regression test that relies on comparing a newly transformed image to
 * a previously generated copy of the same image to detect changes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegressionTest {
  // Intentionally empty.
}
