package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

@SuppressWarnings("rawtypes")
class EngineKey implements Key {
    private static final String FORMAT = "UTF-8";

    private final String id;
    private final int width;
    private final int height;
    private final ResourceDecoder cacheDecoder;
    private final ResourceDecoder decoder;
    private final Transformation transformation;
    private final ResourceEncoder encoder;
    private final ResourceTranscoder transcoder;
    private final Encoder sourceEncoder;
    private String stringKey;
    private int hashCode;
    private OriginalEngineKey originalKey;

    public EngineKey(String id, int width, int height, ResourceDecoder cacheDecoder, ResourceDecoder decoder,
            Transformation transformation, ResourceEncoder encoder, ResourceTranscoder transcoder,
            Encoder sourceEncoder) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.cacheDecoder = cacheDecoder;
        this.decoder = decoder;
        this.transformation = transformation;
        this.encoder = encoder;
        this.transcoder = transcoder;
        this.sourceEncoder = sourceEncoder;
    }

    public Key getOriginalKey() {
        if (originalKey == null) {
            originalKey = new OriginalEngineKey(id);
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
        } else if (height != engineKey.height) {
            return false;
        } else if (width != engineKey.width) {
            return false;
        } else  if (!transformation.getId().equals(engineKey.transformation.getId())) {
            return false;
        } else if (!decoder.getId().equals(engineKey.decoder.getId())) {
            return false;
        } else if (!cacheDecoder.getId().equals(engineKey.cacheDecoder.getId())) {
            return false;
        } else if (!encoder.getId().equals(engineKey.encoder.getId())) {
            return false;
        } else if (!transcoder.getId().equals(engineKey.transcoder.getId())) {
            return false;
        } else {
            return sourceEncoder.getId().equals(engineKey.sourceEncoder.getId());
        }
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = id.hashCode();
            hashCode = 31 * hashCode + width;
            hashCode = 31 * hashCode + height;
            hashCode = 31 * hashCode + cacheDecoder.getId().hashCode();
            hashCode = 31 * hashCode + decoder.getId().hashCode();
            hashCode = 31 * hashCode + transformation.getId().hashCode();
            hashCode = 31 * hashCode + encoder.getId().hashCode();
            hashCode = 31 * hashCode + transcoder.getId().hashCode();
            hashCode = 31 * hashCode + sourceEncoder.getId().hashCode();
        }
        return hashCode;
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
                .append(transcoder.getId())
                .append(sourceEncoder)
                .toString();
        }
        return stringKey;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
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
        messageDigest.update(sourceEncoder.getId().getBytes(FORMAT));
    }
}
