package com.bumptech.glide.util;

import java.util.Collection;

/**
 * Contains common assertions.
 */
public final class Preconditions {

  private Preconditions() {
    // Utility class.
  }

  public static void checkArgument(boolean expression, String message) {
    if (!expression) {
      throw new IllegalArgumentException(message);
    }
  }

  public static <T> T checkNotNull(T arg) {
    return checkNotNull(arg, "Argument must not be null");
  }

  public static <T> T checkNotNull(T arg, String message) {
    if (arg == null) {
      throw new NullPointerException(message);
    }
    return arg;
  }

  public static <T extends Collection<Y>, Y> T checkNotEmpty(T collection) {
    if (collection.isEmpty()) {
      throw new IllegalArgumentException("Must not be empty.");
    }
    return collection;
  }
}
