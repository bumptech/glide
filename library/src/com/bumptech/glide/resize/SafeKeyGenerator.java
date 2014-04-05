package com.bumptech.glide.resize;

import com.bumptech.glide.util.LruCache;
import com.bumptech.glide.util.Util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SafeKeyGenerator {
    private final LruCache<String, String> loadIdToSafeHash = new LruCache<String, String>(250);

    public String getSafeKey(String key) {
        String safeKey = loadIdToSafeHash.get(key);
        if (safeKey == null) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                messageDigest.update(key.getBytes("UTF-8"));
                safeKey = Util.sha256BytesToHex(messageDigest.digest());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return safeKey;
    }
}
