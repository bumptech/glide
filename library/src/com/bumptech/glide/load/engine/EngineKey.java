package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class EngineKey implements Key {
    private static final String FORMAT = "UTF-8";

    private final String id;
    private final int width;
    private final int height;
    private final ResourceDecoder cacheDecoder;
    private final ResourceDecoder decoder;
    private final Transformation transformation;
    private final ResourceEncoder encoder;
    private String stringKey;

    public EngineKey(String id, int width, int height, ResourceDecoder cacheDecoder, ResourceDecoder decoder,
            Transformation transformation, ResourceEncoder encoder) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.cacheDecoder = cacheDecoder;
        this.decoder = decoder;
        this.transformation = transformation;
        this.encoder = encoder;
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
        }
        if (height != engineKey.height) {
            return false;
        }
        if (width != engineKey.width) {
            return false;
        }
        if (!transformation.getId().equals(engineKey.transformation
                .getId())) {
            return false;
        }
        if (!decoder.getId().equals(engineKey.decoder
                .getId())) {
            return false;
        }
        if (!cacheDecoder.getId().equals(engineKey.cacheDecoder
                .getId())) {
            return false;
        }
        if (!encoder.getId().equals(engineKey.encoder
                .getId())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + cacheDecoder.getId().hashCode();
        result = 31 * result + decoder.getId().hashCode();
        result = 31 * result + transformation.getId().hashCode();
        result = 31 * result + encoder.getId().hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (stringKey == null) {
            stringKey = new StringBuilder()
                .append(id)
                .append(width)
                .append(height)
                .append(cacheDecoder.getId())
                .append(decoder.getId())
                .append(transformation.getId())
                .append(encoder.getId())
                .toString();
        }
        return stringKey;
    }

    @Override
    public void update(MessageDigest messageDigest) throws UnsupportedEncodingException {
        byte[] dimensions = ByteBuffer.allocate(8)
                .putInt(width)
                .putInt(height)
                .array();
        messageDigest.update(id.getBytes(FORMAT));
        messageDigest.update(dimensions);
        messageDigest.update(cacheDecoder.getId().getBytes(FORMAT));
        messageDigest.update(decoder.getId().getBytes(FORMAT));
        messageDigest.update(transformation.getId().getBytes(FORMAT));
        messageDigest.update(encoder.getId().getBytes(FORMAT));
    }
}
