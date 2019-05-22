package com.bumptech.glide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies methods in {@link GlideExtension} annotated classes that extend {@code
 * com.bumptech.glide.RequestManager}.
 *
 * <p>If one or more method is found with this annotation, an additional API entry point that
 * exposes a generated {@code com.bumptech.glide.RequestManager} subclass will be created. The
 * generated API entry point acts as a drop in replacement for Glide. Glide.with(fragment) becomes
 * GlideApp.with(fragment). Although the Glide.with variant will still be available, only the new
 * API entry point will provide access to these additional methods.
 *
 * <p>The name of the API entry point created when one of these methods is found can be controlled
 * by {@link GlideModule#glideName()}.
 *
 * <p>Methods with this annotation will only be found if they are contained in a class annotated
 * with {@link GlideExtension}.
 *
 * <p>Methods annotated with GlideType must have a single parameter. The type of the single
 * parameter must be {@code com.bumptech.glide.RequestBuilder}, with a type matching the value of
 * {@link #value()}.
 *
 * <p>Compilation will fail if a method annotated with this method is identical to a method in
 * {@code com.bumptech.glide.RequestManager}
 */
@Target(ElementType.METHOD)
// Needs to be parsed from class files in JAR.
@Retention(RetentionPolicy.CLASS)
public @interface GlideType {

  /**
   * A Resource class name, like GifDrawable.class, Bitmap.class etc.
   *
   * <p>Must match the type of the {@code com.bumptech.glide.RequestBuilder} parameter in the
   * annotated method.
   */
  Class<?> value();
}
