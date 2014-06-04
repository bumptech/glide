package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.Resource;

public class UnitTranscoder implements ResourceTranscoder {
    private static final UnitTranscoder UNIT_TRANSCODER = new UnitTranscoder();

    @SuppressWarnings("unchecked")
    public static <Z, R> ResourceTranscoder<Z, R> get() {
        return UNIT_TRANSCODER;
    }

    @Override
    public Resource transcode(Resource toTranscode) {
        return toTranscode;
    }

    @Override
    public String getId() {
        return "UnitTranscoder.com.bumptech.glide.load.data.transcode";
    }
}
