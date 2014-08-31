package com.bumptech.glide.load.resource.bytes;

import com.bumptech.glide.load.engine.Resource;

/**
 * An {@link com.bumptech.glide.load.engine.Resource} wrapping a byte array.
 */
public class BytesResource extends Resource<byte[]> {
    private byte[] bytes;

    public BytesResource(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public byte[] get() {
        return bytes;
    }

    @Override
    public int getSize() {
        return bytes.length;
    }

    @Override
    protected void recycleInternal() {
        // Do nothing.
    }
}
