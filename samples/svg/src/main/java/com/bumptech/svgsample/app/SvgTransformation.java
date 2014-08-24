package com.bumptech.svgsample.app;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;

public class SvgTransformation implements Transformation<Svg> {

    @Override
    public Resource<Svg> transform(Resource<Svg> resource, int outWidth, int outHeight) {
        Svg svg = resource.get();
        Svg transformed = svg; // or really do something here.
        return new SvgResource(transformed);
    }

    @Override
    public String getId() {
        return "MyTransformation"; // Some id to be mixed in to the cache key
    }
}
