package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.Transformation;

public class GifDataTransformation implements Transformation<GifData> {
    private Transformation<Bitmap> wrapped;

    public GifDataTransformation(Transformation<Bitmap> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Resource<GifData> transform(Resource<GifData> resource, int outWidth, int outHeight) {
        GifData data = resource.get();
        Transformation<Bitmap> newTransformation =
                new MultiTransformation<Bitmap>(data.getFrameTransformation(), wrapped);
        data.setFrameTransformation(newTransformation);
        return resource;
    }

    @Override
    public String getId() {
        return wrapped.getId();
    }
}
