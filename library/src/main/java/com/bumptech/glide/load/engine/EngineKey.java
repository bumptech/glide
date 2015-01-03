package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.util.Preconditions;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

@SuppressWarnings("rawtypes")
class EngineKey implements Key {
    private final String id;
    private final int width;
    private final int height;
    private final Transformation transformation;
    private final Class<?> resourceClass;
    private final Class<?> transcodeClass;
    private final Key signature;
    private int hashCode;
    private Key originalKey;

    public EngineKey(String id, Key signature, int width, int height, Transformation transformation,
            Class<?>  resourceClass,  Class<?> transcodeClass) {
        this.id = Preconditions.checkNotNull(id);
        this.signature = Preconditions.checkNotNull(signature, "Signature must not be null");
        this.width = width;
        this.height = height;
        this.transformation = Preconditions.checkNotNull(transformation, "Transformation must not be null");
        this.resourceClass = Preconditions.checkNotNull(resourceClass, "Resource class must not be null");
        this.transcodeClass = Preconditions.checkNotNull(transcodeClass, "Transcode class must not be null");

        Preconditions.checkNotNull(transformation.getId(), "Transformation id must not be null");
    }

    public Key getOriginalKey() {
        if (originalKey == null) {
            originalKey = new OriginalKey(id, signature);
        }
        return originalKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EngineKey engineKey = (EngineKey) o;

        if (!id.equals(engineKey.id)) {
            return false;
        } else if (!signature.equals(engineKey.signature)) {
            return false;
        } else if (height != engineKey.height) {
            return false;
        } else if (width != engineKey.width) {
            return false;
        } else if (!transformation.getId().equals(engineKey.transformation.getId())) {
            return false;
        } else if (resourceClass != engineKey.resourceClass) {
            return false;
        } else if (transcodeClass != engineKey.transcodeClass) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = id.hashCode();
            hashCode = 31 * hashCode + signature.hashCode();
            hashCode = 31 * hashCode + width;
            hashCode = 31 * hashCode + height;
            hashCode = 31 * hashCode + transformation.getId().hashCode();
            hashCode = 31 * hashCode + resourceClass.hashCode();
            hashCode = 31 * hashCode + transcodeClass.hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return "EngineKey{"
                + "id='" + id + '\''
                + ", width=" + width
                + ", height=" + height
                + ", transformation=" + transformation
                + ", resourceClass=" + resourceClass
                + ", transcodeClass=" + transcodeClass
                + ", signature=" + signature
                + ", originalKey=" + originalKey
                + '}';
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
        byte[] dimensions = ByteBuffer.allocate(8)
                .putInt(width)
                .putInt(height)
                .array();
        signature.updateDiskCacheKey(messageDigest);
        messageDigest.update(id.getBytes(STRING_CHARSET_NAME));
        messageDigest.update(dimensions);
        messageDigest.update(transformation.getId().getBytes(STRING_CHARSET_NAME));
    }
}
