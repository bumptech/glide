package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

public class OriginalEngineKey implements Key {
    private String id;

    public OriginalEngineKey(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OriginalEngineKey)) {
            return false;
        }

        OriginalEngineKey that = (OriginalEngineKey) o;

        if (!id.equals(that.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
        messageDigest.update(id.getBytes("UTF-8"));
    }
}
