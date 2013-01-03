/*
 * Copyright (c) 2012 Bump Technologies Inc. All rights reserved.
 */
package com.bumptech.photos.resize;

import android.graphics.Bitmap;
import android.os.Handler;
import com.bumptech.photos.cache.SizedBitmapCache;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author sam
 *
 */
public class ResizeJobGenerator {

    private final SizedBitmapCache bitmapCache;

    public ResizeJobGenerator(Handler mainHandler) {
        this(mainHandler, null);
    }

    public ResizeJobGenerator(Handler mainHandler, SizedBitmapCache bitmapCache){
        this.bitmapCache = bitmapCache;
    }

    public Bitmap resizeCenterCrop(final String path, final int width, final int height){
        final Bitmap streamed = Utils.streamIn(path, width, height);

        if (streamed.getWidth() == width && streamed.getHeight() == height) {
            return streamed;
        }

        return Utils.centerCrop(getRecycled(width, height), streamed, width, height);
    }

    public Bitmap fitInSpace(final String path, final int width, final int height){
        final Bitmap streamed = Utils.streamIn(path, width > height ? 1 : width, height > width ? 1 : height);
        return Utils.fitInSpace(streamed, width, height);
    }

    public Bitmap loadApproximate(final String path, final int width, final int height){
        return Utils.streamIn(path, width, height);
    }

    public Bitmap loadAsIs(final InputStream is1, final InputStream is2) {
        int[] dimens = new int[] {-1, -1};
        try {
            dimens = Utils.getDimension(is1);
        } finally {
            try {
                is1.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        Bitmap resized = null;
        try {
            resized = Utils.load(is2, getRecycled(dimens));
        } finally {
            try {
                is2.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return resized;
    }

    public Bitmap loadAsIs(final String path, final int width, final int height) {
        return Utils.load(path, getRecycled(width, height));
    }

    public Bitmap loadAsIs(final String path){
        int[] dimens = Utils.getDimensions(path);
        return Utils.load(path, getRecycled(dimens));
    }

    private Bitmap getRecycled(int[] dimens) {
        return getRecycled(dimens[0], dimens[1]);
    }

    private Bitmap getRecycled(int width, int height) {
        Bitmap result = null;
        if (bitmapCache != null) {
            result = bitmapCache.get(width, height);
        }
        return result;
    }
}
