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

  private static String calculateHexStringDigest(Key key) {
     try {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        key.updateDiskCacheKey(messageDigest);
        return Util.sha256BytesToHex(messageDigest.digest());
      } catch (NoSuchAlgorithmException e) {
       throw new RuntimeException(e);
      }
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
