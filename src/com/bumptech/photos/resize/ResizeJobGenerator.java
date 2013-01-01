/*
 * Copyright (c) 2012 Bump Technologies Inc. All rights reserved.
 */
package com.bumptech.photos.resize;

import android.graphics.Bitmap;
import android.os.Handler;
import com.bumptech.photos.LoadedCallback;
import com.bumptech.photos.cache.SizedBitmapCache;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author sam
 *
 */
public class ResizeJobGenerator {

    private Handler mainHandler;
    private final SizedBitmapCache bitmapCache;

    public ResizeJobGenerator(Handler mainHandler) {
        this(mainHandler, null);
    }

    public ResizeJobGenerator(Handler mainHandler, SizedBitmapCache bitmapCache){
        this.mainHandler = mainHandler;
        this.bitmapCache = bitmapCache;
    }

    public Runnable resizeCenterCrop(final String path, final int width, final int height, LoadedCallback callback){
        return new StreamResizeRunnable(callback) {

            @Override
            public Bitmap getRecycledBitmap() {
                return bitmapCache.get(width, height);
            }

            @Override
            public Bitmap resize(Bitmap recycled) {
                Bitmap streamed = Utils.streamIn(path, width, height);

                if (streamed.getWidth() == width && streamed.getHeight() == height) {
                    return streamed;
                } else {
                    return Utils.centerCrop(recycled, streamed, width, height);
                }
            }
        };
    }

    public Runnable fitInSpace(final String path, final int width, final int height, LoadedCallback callback){
        return new SimpleStreamResizeRunnable(callback) {

            @Override
            public Bitmap resize(Bitmap recycled) {
                final Bitmap streamed = Utils.streamIn(path, width > height ? 1 : width, height > width ? 1 : height);
                return Utils.fitInSpace(streamed, width, height);
            }
        };
    }

    public Runnable loadApproximate(final String path, final int width, final int height, LoadedCallback callback){
        return new SimpleStreamResizeRunnable(callback) {

            @Override
            public Bitmap resize(Bitmap recycled) {
                return Utils.streamIn(path, width, height);
            }
        };
    }

    public Runnable loadAsIs(final InputStream is1, final InputStream is2, final LoadedCallback callback) {
        return new StreamResizeRunnable(callback) {

            @Override
            public Bitmap getRecycledBitmap() {
                int[] dimens = new int[] {-1, -1};
                try {
                    dimens = Utils.getDimension(is1);
                } finally {
                    try {
                        is1.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return bitmapCache.get(dimens[0], dimens[1]);
            }

            @Override
            public Bitmap resize(Bitmap recycled) {
                Bitmap resized = null;
                try {
                    resized = Utils.load(is2, recycled);
                } finally {
                    try {
                        is2.close();
                    } catch (IOException e) {
                    }
                }
                return resized;
            }
        };
    }

    public Runnable loadAsIs(final String path, final int width, final int height, LoadedCallback cb) {
        return new StreamResizeRunnable(cb) {
            @Override
            public Bitmap getRecycledBitmap() {
                return bitmapCache.get(width, height);
            }

            @Override
            public Bitmap resize(Bitmap recycled) {
                return Utils.load(path, recycled);
            }
        };
    }

    public Runnable loadAsIs(final String path, LoadedCallback callback){
        return new StreamResizeRunnable(callback) {
            @Override
            public Bitmap getRecycledBitmap() {
                int[] dimens = Utils.getDimensions(path);
                return bitmapCache.get(dimens[0], dimens[1]);
            }

            @Override
            public Bitmap resize(Bitmap recycled) {
                return Utils.load(path, recycled);
            }
        };
    }

    private abstract class SimpleStreamResizeRunnable extends StreamResizeRunnable {

        public SimpleStreamResizeRunnable(LoadedCallback callback) {
            super(callback);
        }

        @Override
        public final Bitmap getRecycledBitmap() {
            return null;
        }
    }

    private abstract class StreamResizeRunnable implements Runnable {
        private final LoadedCallback callback;

        public StreamResizeRunnable(LoadedCallback callback) {
            this.callback = callback;
        }

        @Override
        public final void run() {
            try {
                Bitmap recycled = null;
                if (bitmapCache != null) {
                    recycled = getRecycledBitmap();
                }
                final Bitmap result = resize(recycled);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLoadCompleted(result);
                    }
                });
            } catch (final Exception e) {
                e.printStackTrace();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLoadFailed(e);
                    }
                });
            }
        }

        public abstract Bitmap getRecycledBitmap();

        public abstract Bitmap resize(Bitmap recycled);
    }
}
