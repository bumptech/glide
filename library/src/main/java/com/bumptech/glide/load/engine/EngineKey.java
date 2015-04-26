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
    private static final String EMPTY_LOG_STRING = "";
    private final String id;
    private final int width;
    private final int height;
    private final ResourceDecoder cacheDecoder;
    private final ResourceDecoder decoder;
    private final Transformation transformation;
    private final ResourceEncoder encoder;
    private final ResourceTranscoder transcoder;
    private final Encoder sourceEncoder;
    private final Key signature;
    private String stringKey;
    private int hashCode;
    private Key originalKey;

    public EngineKey(String id, Key signature, int width, int height, ResourceDecoder cacheDecoder,
            ResourceDecoder decoder, Transformation transformation, ResourceEncoder encoder,
            ResourceTranscoder transcoder, Encoder sourceEncoder) {
        this.id = id;
        this.signature = signature;
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
        } else if (transformation == null ^ engineKey.transformation == null) {
            return false;
        } else if (transformation != null && !transformation.getId().equals(engineKey.transformation.getId())) {
            return false;
        } else if (decoder == null ^ engineKey.decoder == null) {
            return false;
        } else if (decoder != null && !decoder.getId().equals(engineKey.decoder.getId())) {
            return false;
        } else if (cacheDecoder == null ^ engineKey.cacheDecoder == null) {
            return false;
        } else if (cacheDecoder != null && !cacheDecoder.getId().equals(engineKey.cacheDecoder.getId())) {
            return false;
        } else if (encoder == null ^ engineKey.encoder == null) {
            return false;
        } else if (encoder != null && !encoder.getId().equals(engineKey.encoder.getId())) {
            return false;
        } else if (transcoder == null ^ engineKey.transcoder == null) {
            return false;
        } else if (transcoder != null && !transcoder.getId().equals(engineKey.transcoder.getId())) {
            return false;
        } else if (sourceEncoder == null ^ engineKey.sourceEncoder == null) {
            return false;
        } else if (sourceEncoder != null && !sourceEncoder.getId().equals(engineKey.sourceEncoder.getId())) {
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
            hashCode = 31 * hashCode + (cacheDecoder   != null ? cacheDecoder  .getId().hashCode() : 0);
            hashCode = 31 * hashCode + (decoder        != null ? decoder       .getId().hashCode() : 0);
            hashCode = 31 * hashCode + (transformation != null ? transformation.getId().hashCode() : 0);
            hashCode = 31 * hashCode + (encoder        != null ? encoder       .getId().hashCode() : 0);
            hashCode = 31 * hashCode + (transcoder     != null ? transcoder    .getId().hashCode() : 0);
            hashCode = 31 * hashCode + (sourceEncoder  != null ? sourceEncoder .getId().hashCode() : 0);
        }
        return hashCode;
    }

    @Override
    public String toString() {
        if (stringKey == null) {
            stringKey = new StringBuilder()
                .append("EngineKey{")
                .append(id)
                .append('+')
                .append(signature)
                .append("+[")
                .append(width)
                .append('x')
                .append(height)
                .append("]+")
                .append('\'')
                .append(cacheDecoder   != null ? cacheDecoder  .getId() : EMPTY_LOG_STRING)
                .append('\'')
                .append('+')
                .append('\'')
                .append(decoder        != null ? decoder       .getId() : EMPTY_LOG_STRING)
                .append('\'')
                .append('+')
                .append('\'')
                .append(transformation != null ? transformation.getId() : EMPTY_LOG_STRING)
                .append('\'')
                .append('+')
                .append('\'')
                .append(encoder        != null ? encoder       .getId() : EMPTY_LOG_STRING)
                .append('\'')
                .append('+')
                .append('\'')
                .append(transcoder     != null ? transcoder    .getId() : EMPTY_LOG_STRING)
                .append('\'')
                .append('+')
                .append('\'')
                .append(sourceEncoder  != null ? sourceEncoder .getId() : EMPTY_LOG_STRING)
                .append('\'')
                .append('}')
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
        signature.updateDiskCacheKey(messageDigest);
        messageDigest.update(id.getBytes(STRING_CHARSET_NAME));
        messageDigest.update(dimensions);
        messageDigest.update((cacheDecoder   != null ? cacheDecoder  .getId() : "").getBytes(STRING_CHARSET_NAME));
        messageDigest.update((decoder        != null ? decoder       .getId() : "").getBytes(STRING_CHARSET_NAME));
        messageDigest.update((transformation != null ? transformation.getId() : "").getBytes(STRING_CHARSET_NAME));
        messageDigest.update((encoder        != null ? encoder       .getId() : "").getBytes(STRING_CHARSET_NAME));
        // The Transcoder is not included in the disk cache key because its result is not cached.
        messageDigest.update((sourceEncoder  != null ? sourceEncoder .getId() : "").getBytes(STRING_CHARSET_NAME));
    }
}
