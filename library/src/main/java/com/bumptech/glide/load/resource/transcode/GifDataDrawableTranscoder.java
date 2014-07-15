package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawableResource;

/**
 * An {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} that can transcode an
 * {@link com.bumptech.glide.load.resource.gif.GifData} into an
 * {@link com.bumptech.glide.load.resource.gif.GifDrawable}.
 */
public class GifDataDrawableTranscoder implements ResourceTranscoder<GifData, GifDrawable> {
    @Override
    public Resource<GifDrawable> transcode(Resource<GifData> toTranscode) {
        return new GifDrawableResource(toTranscode);
    }

    @Override
    public String getId() {
        return "GifDataDrawableTranscoder.com.bumptech.glide.load.resource.transcode";
    }
}
