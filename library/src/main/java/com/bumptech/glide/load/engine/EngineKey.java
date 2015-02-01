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
    private final Class<?> resourceClass;
    private final Class<?> transcodeClass;
    private final Key signature;
    private int hashCode;
    private Key originalKey;

    public EngineKey(String id, Key signature, int width, int height, Class<?> resourceClass,
            Class<?>  transcodeClass) {
        this.id = Preconditions.checkNotNull(id);
        this.signature = Preconditions.checkNotNull(signature, "Signature must not be null");
        this.width = width;
        this.height = height;
        this.resourceClass = Preconditions.checkNotNull(resourceClass, "Resource class must not be null");
        this.transcodeClass = Preconditions.checkNotNull(transcodeClass, "Transcode class must not be null");
    }

    public Key getOriginalKey() {
        if (originalKey == null) {
            originalKey = new OriginalKey(id, signature);
        }
        return originalKey;
    }

    public Key getResultKey(Transformation<?> appliedTransformation, Class<?> decodedResourceClass) {
        return new ResultKey(id, signature, width, height, appliedTransformation, decodedResourceClass);
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
                + ", resourceClass=" + resourceClass
                + ", transcodeClass=" + transcodeClass
                + ", signature=" + signature
                + ", originalKey=" + originalKey
                + '}';
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
        throw new UnsupportedOperationException();
    }

    private static class ResultKey implements Key {
        private final String id;
        private final Key signature;
        private final int width;
        private final int height;
        private final Transformation<?> appliedTransformation;
        private final Class<?> decodedResourceClass;

        public ResultKey(String id, Key signature, int width, int height, Transformation<?> appliedTransformation,
                Class<?> decodedResourceClass) {
            this.id = id;
            this.signature = signature;
            this.width = width;
            this.height = height;
            this.appliedTransformation = appliedTransformation;
            this.decodedResourceClass = decodedResourceClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResultKey resultKey = (ResultKey) o;

            if (height != resultKey.height) return false;
            if (width != resultKey.width) return false;
            if (!appliedTransformation.equals(resultKey.appliedTransformation)) return false;
            if (!decodedResourceClass.equals(resultKey.decodedResourceClass)) return false;
            if (!id.equals(resultKey.id)) return false;
            if (!signature.equals(resultKey.signature)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + signature.hashCode();
            result = 31 * result + width;
            result = 31 * result + height;
            result = 31 * result + appliedTransformation.hashCode();
            result = 31 * result + decodedResourceClass.hashCode();
            return result;
        }

        @Override
        public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
            byte[] dimensions = ByteBuffer.allocate(8)
                    .putInt(width)
                    .putInt(height)
                    .array();
            messageDigest.update(id.getBytes(STRING_CHARSET_NAME));
            signature.updateDiskCacheKey(messageDigest);
            messageDigest.update(dimensions);
            messageDigest.update(appliedTransformation.getId().getBytes(STRING_CHARSET_NAME));
        }
    }
}
