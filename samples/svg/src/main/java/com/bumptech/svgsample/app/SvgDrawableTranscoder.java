package com.bumptech.svgsample.app;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

public class SvgDrawableTranscoder implements ResourceTranscoder<Svg, SvgDrawable> {
    @Override
    public Resource<SvgDrawable> transcode(Resource<Svg> toTranscode) {
        Svg svg = toTranscode.get();
        return new SvgDrawableResource(new SvgDrawable(svg));
    }

    @Override
    public String getId() {
        // If you're planning on having more than one transcoder, add an id, otherwise empty string is fine.
        return "";
    }
}
