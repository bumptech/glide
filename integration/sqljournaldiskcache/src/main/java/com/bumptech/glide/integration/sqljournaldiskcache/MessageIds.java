package com.bumptech.glide.integration.sqljournaldiskcache;

/**
 * A unique set of non-zero message ids to use when requesting that work be done on the disk cache's
 * background thread.
 */
interface MessageIds {
  int ADD_LAST_MODIFIED_KEY = 1;
  int EVICT = 2;
  int RECOVER = 3;
}
