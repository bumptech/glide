package com.bumptech.glide.load.model;

import java.util.Map;

/**
 * An interface for a wrapper for a set of headers to be included in a Glide request.
 * Implementations must implement equals() and hashcode().
 */
public interface Headers {

    /** An empty Headers object that can be used if users don't want to provide headers. */
    Headers NONE = new BasicHeaders.Builder().build();

    Map<String, String> getHeaders();

}
