/*
 * Copyright (c) 2012 Bump Technologies Inc. All rights reserved.
 */
package com.bumptech.photos.resize;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import com.bumptech.photos.cache.SizedBitmapCache;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author sam
 *
 */
public class PhotoStreamResizer {
    private static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;

    private Handler mainHandler;
    private final SizedBitmapCache bitmapCache;

    public interface ResizeCallback {
        void onResizeComplete(Bitmap resized);
        void onResizeFailed(Exception e);
    }

    public PhotoStreamResizer(Handler mainHandler, SizedBitmapCache bitmapCache){
        this.mainHandler = mainHandler;
        this.bitmapCache = bitmapCache;
    }

    public Runnable resizeCenterCrop(final String path, final int width, final int height, ResizeCallback callback){
        return new SimpleStreamResizeRunnable(callback) {
            @Override
            public Bitmap resize(Bitmap recycled) {
                Bitmap streamed = Utils.streamIn(path, width, height);

                if (streamed.getWidth() == width && streamed.getHeight() == height) {
                    return streamed;
                } else {
                    return Utils.centerCrop(streamed, width, height);
                }
            }
        };
    }

    public Runnable fitInSpace(final String path, final int width, final int height, ResizeCallback callback){
        return new SimpleStreamResizeRunnable(callback) {
            @Override
            public Bitmap resize(Bitmap recycled) {
                final Bitmap streamed = Utils.streamIn(path, width > height ? 1 : width, height > width ? 1 : height);
                return Utils.fitInSpace(streamed, width, height);
            }
        };
    }

    public Runnable loadApproximate(final String path, final int width, final int height, ResizeCallback callback){
        return new SimpleStreamResizeRunnable(callback) {

            @Override
            public Bitmap resize(Bitmap recycled) {
                return Utils.streamIn(path, width, height);
            }
        };
    }

    public Runnable loadAsIs(final InputStream is1, final InputStream is2, final ResizeCallback callback) {
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

    public Runnable loadAsIs(final String path, ResizeCallback callback){
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
    };

    private abstract class SimpleStreamResizeRunnable extends StreamResizeRunnable {

        public SimpleStreamResizeRunnable(ResizeCallback callback) {
            super(callback);
        }

        @Override
        public final Bitmap getRecycledBitmap() {
            return null;
        }
    }

    private abstract class StreamResizeRunnable implements Runnable {
        private final PhotoStreamResizer.ResizeCallback callback;

        public StreamResizeRunnable(ResizeCallback callback) {
            this.callback = callback;
        }

        @Override
        public final void run() {
            try {
                Bitmap recycled = null;
                if (CAN_RECYCLE) {
                    recycled = getRecycledBitmap();
                }
                final Bitmap result = resize(recycled);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResizeComplete(result);
                    }
                });
            } catch (final Exception e) {
                e.printStackTrace();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResizeFailed(e);
                    }
                });
            }
        }

        public abstract Bitmap getRecycledBitmap();

        public abstract Bitmap resize(Bitmap recycled);
    }
}
