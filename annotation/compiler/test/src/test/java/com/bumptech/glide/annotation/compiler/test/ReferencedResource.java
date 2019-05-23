package com.bumptech.glide.annotation.compiler.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the method in question is referencing a test resource that it doesn't "own" and
 * should not attempt to regenerate.
 *
 * <p>Used by {@link RegenerateResourcesRule} to ensure that if we are regenerating resources, we're
 * only regenerating them for a single class and only for the single class that has the correct name
 * and directory sequence so that we update the correct file.
 *
 * <p>Ideally this wouldn't be necessary. It would be great if we could find a way to go from the
 * test failure more directly to the actual path of the resource used. Right now we're basically
 * guessing based on this annotation, the class name of the test class, and any values from {@link
 * SubDirectory}. Without this annotation, we'd end up writing files that were never used.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferencedResource {}
