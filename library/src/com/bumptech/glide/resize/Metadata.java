package com.bumptech.glide.resize;

import com.bumptech.glide.resize.load.DecodeFormat;

public class Metadata {
    public static final Metadata DEFAULT = new Metadata(Priority.NORMAL, DecodeFormat.PREFER_RGB_565);

    private final Priority priority;
    private final DecodeFormat decodeFormat;

    public Metadata(Priority priority, DecodeFormat decodeFormat) {
        this.priority = priority;
        this.decodeFormat = decodeFormat;
    }

    public Priority getPriority() {
        return priority;
    }

    public DecodeFormat getDecodeFormat() {
        return decodeFormat;
    }

}
