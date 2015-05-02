package com.bumptech.glide.load.model;

import java.util.Collections;
import java.util.Map;

/**
 * An interface for a wrapper for a set of headers to be included in a Glide request.
 *
 * <p> Implementations must implement equals() and hashcode(). </p>
 */
public interface Headers {

  /** An empty Headers object that can be used if users don't want to provide headers. */
  Headers NONE = new Headers() {
      @Override
      public Map<String, String> getHeaders() {
          return Collections.emptyMap();
      }
  };

  /**
   * Returns a non-null map containing a set of headers to apply to an http request.
   */
  Map<String, String> getHeaders();
}
