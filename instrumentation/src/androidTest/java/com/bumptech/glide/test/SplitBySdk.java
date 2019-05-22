package com.bumptech.glide.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used by {@link BitmapRegressionTester} to generate SDK specific resources to account for
 * differences in Android's image decoding APIs across versions.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SplitBySdk {
  int[] value();
}
