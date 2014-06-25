package com.bumptech.glide.load.resource.bytes;

import com.bumptech.glide.load.engine.Resource;

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
    protected void recycleInternal() {  }
}
