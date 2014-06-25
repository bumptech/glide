package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bytes.BytesResource;
import com.bumptech.glide.load.resource.gif.GifData;

public class GifDataBytesTranscoder implements ResourceTranscoder<GifData, byte[]> {
    @Override
    public Resource<byte[]> transcode(Resource<GifData> toTranscode) {
        GifData gifData = toTranscode.get();
        return new BytesResource(gifData.getData());
    }

    @Override
    public String getId() {
        return "GifDataBytesTranscoder.com.bumptech.glide.load.resource.transcode";
    }
}
