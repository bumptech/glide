package com.bumptech.glide.load;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * A very generic interface that must implement {@link Object#equals(Object)} and {@link Object#hashCode()} to include a
 * set of uniquely identifying information for the object(s) represented by this key. Keys are used as cache keys so
 * they must be unique within a given dataset.
 *
 */
public interface Key {
    /**
     * Adds all uniquely identifying information to the given digest.
     */
    void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException;

    /**
     * {@inheritDoc}
     */
    boolean equals(Object o);

    /**
     * {@inheritDoc}
     */
    int hashCode();
}
