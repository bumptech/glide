package com.bumptech.glide.integration.sqljournaldiskcache;

/**
 * A simple wrapper for obtaining the current time for testing.
 *
 * <p>While this interface exists in lots of libraries, especially internally at Google, there
 * doesn't seem to be a reasonable public version. For now we're just duplicating it again in Glide
 * so that the library can be open sourced.
 */
public interface Clock {
  long currentTimeMillis();
}
