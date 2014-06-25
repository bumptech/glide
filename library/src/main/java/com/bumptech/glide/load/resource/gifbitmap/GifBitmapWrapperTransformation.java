package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDataTransformation;

public class GifBitmapWrapperTransformation implements Transformation<GifBitmapWrapper> {
    private Transformation<Bitmap> bitmapTransformation;
    private Transformation<GifData> gifDataTransformation;

    public GifBitmapWrapperTransformation(Transformation<Bitmap> bitmapTransformation) {
        this(bitmapTransformation, new GifDataTransformation(bitmapTransformation));
    }

    GifBitmapWrapperTransformation(Transformation<Bitmap> bitmapTransformation,
            Transformation<GifData> gifDataTransformation) {
        this.bitmapTransformation = bitmapTransformation;
        this.gifDataTransformation = gifDataTransformation;
    }

    @Override
    public Resource<GifBitmapWrapper> transform(Resource<GifBitmapWrapper> resource, int outWidth, int outHeight) {
        Resource<Bitmap> bitmapResource = resource.get().getBitmapResource();
        Resource<GifData> gifResource = resource.get().getGifResource();
        if (bitmapResource != null && bitmapTransformation != null) {
            Resource<Bitmap> transformed = bitmapTransformation.transform(bitmapResource, outWidth, outHeight);
            if (transformed != bitmapResource) {
                GifBitmapWrapper gifBitmap = new GifBitmapWrapper(transformed, resource.get().getGifResource());
                return new GifBitmapWrapperResource(gifBitmap);
            }
        } else if (gifResource != null && gifDataTransformation != null) {
            Resource<GifData> transformed = gifDataTransformation.transform(gifResource, outWidth, outHeight);
            if (transformed != gifResource) {
                GifBitmapWrapper gifBitmap = new GifBitmapWrapper(resource.get().getBitmapResource(), transformed);
                return new GifBitmapWrapperResource(gifBitmap);
            }
        }
        return resource;
    }

    @Override
    public String getId() {
        return bitmapTransformation.getId();
    }
}
