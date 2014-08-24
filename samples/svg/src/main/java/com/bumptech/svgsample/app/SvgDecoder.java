package com.bumptech.svgsample.app;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

import java.io.IOException;
import java.io.InputStream;

public class SvgDecoder implements ResourceDecoder<InputStream,Svg> {

    @Override
    public Resource<Svg> decode(InputStream source, int width, int height) throws IOException {
        return new SvgResource(Svg.fromStream(source));
    }

    @Override
    public String getId() {
        return ""; // Or if you're planning on have multiple give it some unique id.
    }
}
