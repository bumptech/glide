package com.bumptech.glide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate a class that extends Glide's public API.
 *
 * @see GlideOption
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface GlideExtension {}
