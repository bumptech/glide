package com.bumptech.glide.util;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Collection;

/** Contains common assertions. */
public final class Preconditions {

  private Preconditions() {
    // Utility class.
  }

  public static void checkArgument(boolean expression) {
    checkArgument(expression, /* message= */ "");
  }

  public static void checkArgument(boolean expression, @NonNull String message) {
    if (!expression) {
      throw new IllegalArgumentException(message);
    }
  }

  @NonNull
  public static <T> T checkNotNull(@Nullable T arg) {
    return checkNotNull(arg, "Argument must not be null");
  }

  @NonNull
  public static <T> T checkNotNull(@Nullable T arg, @NonNull String message) {
    if (arg == null) {
      throw new NullPointerException(message);
    }
    return arg;
  }

  @NonNull
  public static String checkNotEmpty(@Nullable String string) {
    if (TextUtils.isEmpty(string)) {
      throw new IllegalArgumentException("Must not be null or empty");
    }
    return string;
  }

  @NonNull
  public static <T extends Collection<Y>, Y> T checkNotEmpty(@NonNull T collection) {
    if (collection.isEmpty()) {
      throw new IllegalArgumentException("Must not be empty.");
    }
    return collection;
  }
}
