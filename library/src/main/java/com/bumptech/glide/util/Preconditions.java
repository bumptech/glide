package com.bumptech.glide.util;

import java.util.Collection;

/**
 * Contains common assertions.
 */
public class Preconditions {

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
