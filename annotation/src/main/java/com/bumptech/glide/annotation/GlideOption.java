package com.bumptech.glide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies methods in {@link GlideExtension} annotated classes that extend
 * {@link com.bumptech.glide.request.RequestOptions}.
 *
 * <p>All annotated methods will be added to a single
 * {@link com.bumptech.glide.request.RequestOptions} implementation generated per application.
 * Overlapping method names in different extensions may cause errors at compile time.
 *
 * <p>Static equivalents of annotated methods will also be generated.
 *
 * <p>Methods with this annotation will only be found if they belong to classes annotated with
 * {@link GlideExtension}.
 */
@Target(ElementType.METHOD)
// Needs to be parsed from class files in JAR.
@Retention(RetentionPolicy.CLASS)
public @interface GlideOption {
   /** Does not intend to override a method in a super class. */
  int OVERRIDE_NONE = 0;
  /** Expects to call super and then add additional functionality to an overridden method. */
  int OVERRIDE_EXTEND = 1;
  /** Expects to not call super and replace an overridden method. */
  int OVERRIDE_REPLACE = 2;

  /**
   * Determines how and whether a generated method should extend a method from it's parent.
   *
   * <p>Must be one of {@link #OVERRIDE_NONE}, {@link #OVERRIDE_EXTEND}, {@link #OVERRIDE_REPLACE}.
   *
   * <p>The extended method is determined by String and argument matching against methods in the
   * extended class. If {@link #OVERRIDE_NONE} is used and the method and arguments match a method
   * in the extended class, a compile time error will result. Similarly if any other override type
   * is used and no method/arguments in the extended class match, a compile time error will result.
   */
  int override() default OVERRIDE_NONE;

  /**
   * Sets the name for the generated static version of this method.
   *
   * <p>If this value is not set, the static method name is just the original method name with "Of"
   * appended.
   */
  String staticMethodName() default "";

  /**
   * {@code true} to indicate that it's safe to statically memoize the result of this method using
   * {@link com.bumptech.glide.request.RequestOptions#autoClone()}.
   *
   * <p>This method should only be used for no-arg methods where there's only a single possible
   * value.
   *
   * <p>Memoization can save object allocations for frequently used options.
   */
  boolean memoizeStaticMethod() default false;

  /**
   * {@code true} to prevent a static builder method from being generated.
   *
   * <p>By default static methods are generated for all methods annotated with
   * {@link GlideOption}. These static factory methods allow for a cleaner API when used
   * with {@link com.bumptech.glide.RequestBuilder#apply}. The static factory method by default
   * simply creates a new {@link com.bumptech.glide.request.RequestOptions} object, calls the
   * instance version of the method on it and returns it. For example:
   * <pre>
   * <code>
   * public static GlideOptions noAnimation() {
   *   return new GlideOptions().dontAnimate();
   * }
   * </code>
   * </pre>
   *
   * @see #memoizeStaticMethod()
   * @see #staticMethodName()
   */
  boolean skipStaticMethod() default false;
}
