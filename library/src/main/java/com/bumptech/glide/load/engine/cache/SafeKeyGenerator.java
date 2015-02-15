package com.bumptech.glide.load.engine.cache;

import android.util.Log;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.util.LruCache;
import com.bumptech.glide.util.Util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A class that generates and caches safe and unique string file names from {@link
 * com.bumptech.glide.load.Key}s.
 */
class SafeKeyGenerator {
  private static final String TAG = "KeyGen";
  private final LruCache<Key, String> loadIdToSafeHash = new LruCache<>(1000);

  public String getSafeKey(Key key) {
    String safeKey;
    synchronized (loadIdToSafeHash) {
      safeKey = loadIdToSafeHash.get(key);
    }
    if (safeKey == null) {
      try {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        key.updateDiskCacheKey(messageDigest);
        safeKey = Util.sha256BytesToHex(messageDigest.digest());
      } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
          Log.e(TAG, "Failed to create cache key", e);
        }
      }
      synchronized (loadIdToSafeHash) {
        loadIdToSafeHash.put(key, safeKey);
      }
    }
    return safeKey;
  }
}
