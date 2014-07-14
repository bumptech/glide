package com.bumptech.glide.samples.giphy;

import android.content.Context;

import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;

import java.io.InputStream;

public class GiphyModelLoader extends BaseGlideUrlLoader<Api.GifResult> {
    public static class Factory implements ModelLoaderFactory<Api.GifResult, InputStream> {

        @Override
        public ModelLoader<Api.GifResult, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new GiphyModelLoader(context);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    public GiphyModelLoader(Context context) {
        super(context);
    }

    @Override
    protected String getUrl(Api.GifResult model, int width, int height) {
        Api.GifImage fixedHeight = model.images.fixed_height_downsampled;
        int fixedHeightDifference = getDifference(fixedHeight, width, height);
        Api.GifImage fixedWidth = model.images.fixed_width_downsampled;
        int fixedWidthDifference = getDifference(fixedWidth, width, height);
        if (fixedHeightDifference < fixedWidthDifference) {
            return fixedHeight.url;
        } else {
            return fixedWidth.url;
        }
    }

    private static int getDifference(Api.GifImage gifImage, int width, int height) {
        return Math.abs(width - gifImage.width) + Math.abs(height - gifImage.height);

    }
}
