package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.util.LruCache;
import com.bumptech.glide.util.Util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A class that generates and caches safe and unique string file names from {@link
 * com.bumptech.glide.load.Key}s.
 */
public class SafeKeyGenerator {
  private final LruCache<Key, String> loadIdToSafeHash = new LruCache<>(1000);

  private static final MessageDigest MESSAGE_DIGEST;

  static {
    try {
      MESSAGE_DIGEST = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static synchronized String calculateHexStringDigest(Key key) {
    key.updateDiskCacheKey(MESSAGE_DIGEST);

    // calling digest() will automatically reset()
    return Util.sha256BytesToHex(MESSAGE_DIGEST.digest());
  }

  public String getSafeKey(Key key) {
    String safeKey;
    synchronized (loadIdToSafeHash) {
      safeKey = loadIdToSafeHash.get(key);
    }
    if (safeKey == null) {
      safeKey = calculateHexStringDigest(key);
    }
    synchronized (loadIdToSafeHash) {
      loadIdToSafeHash.put(key, safeKey);
    }
    return safeKey;
  }
}
