package com.bumptech.glide.load;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * An interface that uniquely identifies some set of data. Implementations must implement {@link Object#equals(Object)}
 * and {@link Object#hashCode()}. Implementations are generally expected to add all uniquely identifying information
 * used in in {@link java.lang.Object#equals(Object)}} and {@link Object#hashCode()}} to the given
 * {@link java.security.MessageDigest} in {@link #updateDiskCacheKey(java.security.MessageDigest)}}, although this
 * requirement is not as strict for partial cache key signatures.
 */
public interface Key {
    String STRING_CHARSET_NAME = "UTF-8";

    /**
     * Adds all uniquely identifying information to the given digest.
     *
     * <p>
     *     Note - Using {@link java.security.MessageDigest#reset()} inside of this method will result in undefined
     *     behavior.
     * </p>
     */
    void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException;

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
