package com.bumptech.glide.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Indicates that target's visibility can be relaxed to avoid synthetic methods. */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE})
public @interface Synthetic {}
