package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;

import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

/**
 * An {@link com.bumptech.glide.load.Transformation} that wraps a transformation for a {@link Bitmap}
 * and can apply it to every frame of any {@link com.bumptech.glide.load.resource.gif.GifDrawable}.
 */
public class GifDrawableTransformation implements Transformation<GifDrawable> {
    private Transformation<Bitmap> wrapped;
    private BitmapPool bitmapPool;

    public GifDrawableTransformation(Transformation<Bitmap> wrapped, BitmapPool bitmapPool) {
        this.wrapped = wrapped;
        this.bitmapPool = bitmapPool;
    }

    @Override
    public Resource<GifDrawable> transform(Resource<GifDrawable> resource, int outWidth, int outHeight) {
        GifDrawable drawable = resource.get();
        @SuppressWarnings("unchecked")
        Transformation<Bitmap> newTransformation =
                new MultiTransformation<Bitmap>(drawable.getFrameTransformation(), wrapped);

        // The drawable needs to be initialized with the correct width and height in order for a view displaying it
        // to end up with the right dimensions. Since our transformations may arbitrarily modify the dimensions of
        // our gif, here we create a stand in for a frame and pass it to the transformation to see what the final
        // transformed dimensions will be so that our drawable can report the correct intrinsict width and height.
        Bitmap toTest = bitmapPool.get(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                Bitmap.Config.RGB_565);
        if (toTest == null) {
            toTest = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                    Bitmap.Config.RGB_565);
        }

        Resource<Bitmap> bitmapResource = new BitmapResource(toTest, bitmapPool);
        Resource<Bitmap> transformed = newTransformation.transform(bitmapResource, outWidth, outHeight);
        if (bitmapResource != transformed) {
            bitmapResource.recycle();
        }
        Bitmap bitmap = transformed.get();
        final int transformedWidth = bitmap.getWidth();
        final int transformedHeight = bitmap.getHeight();
        transformed.recycle();

        drawable.setFrameTransformation(newTransformation, transformedWidth, transformedHeight);
        return resource;
    }

    @Override
    public String getId() {
        return wrapped.getId();
    }
}
