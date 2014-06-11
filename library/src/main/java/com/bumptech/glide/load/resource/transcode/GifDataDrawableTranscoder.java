package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawableResource;

public class GifDataDrawableTranscoder implements ResourceTranscoder<GifData, GifDrawable> {
    @Override
    public Resource<GifDrawable> transcode(Resource<GifData> toTranscode) {
        return new GifDrawableResource(toTranscode);
    }

    @Override
    public String getId() {
        return "GifDataDrawableTranscoder.com.bumotech.glide.load.resource.transcode";
    }
}
