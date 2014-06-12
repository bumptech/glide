package com.bumptech.glide.load.resource.gifbitmap;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifResource;

public class GifBitmapTransformation implements Transformation<GifBitmap> {
    private Context context;
    private Transformation<Bitmap> wrapped;

    public GifBitmapTransformation(Context context, Transformation<Bitmap> wrapped) {
        this.context = context;
        this.wrapped = wrapped;
    }

    @Override
    public Resource<GifBitmap> transform(Resource<GifBitmap> resource, int outWidth, int outHeight) {
        Resource<Bitmap> bitmapResource = resource.get().getBitmapResource();
        if (bitmapResource != null) {
            Resource<Bitmap> transformed = wrapped.transform(bitmapResource, outWidth, outHeight);
            if (transformed != bitmapResource) {
                GifBitmap gifBitmap = new GifBitmap(transformed, null);
                return new GifBitmapResource(gifBitmap);
            }
        } else {
            //TODO: this should be pushed down into a GifData transformation?
            Resource<GifData> gifResource = resource.get().getGifResource();
            GifData gifData = gifResource.get();
            Transformation<Bitmap> newTransformation =
                    new MultiTransformation<Bitmap>(gifData.getFrameTransformation(), wrapped);
            gifData.setFrameTransformation(newTransformation);
            return new GifBitmapResource(new GifBitmap(null, new GifResource(gifData)));
        }
        return resource;
    }

    @Override
    public String getId() {
        return wrapped.getId();
    }
}
