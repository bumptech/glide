/*
 * Copyright (c) 2012 Bump Technologies Inc. All rights reserved.
 */
package com.bumptech.photos.resize;

import android.graphics.Bitmap;
import android.os.Handler;

import java.io.InputStream;

/**
 * @author sam
 *
 */
public class PhotoStreamResizer {

    private Handler mainHandler;

    public interface ResizeCallback {
        void onResizeComplete(Bitmap resized);
        void onResizeFailed(Exception e);
    }

    public PhotoStreamResizer(Handler mainHandler){
        this.mainHandler = mainHandler;
    }

    public Runnable resizeCenterCrop(final String path, final int width, final int height, ResizeCallback callback){
        return new StreamResizeRunnable(callback) {
            @Override
            public Bitmap resize() {
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
        return new StreamResizeRunnable(callback) {
            @Override
            public Bitmap resize() {
                final Bitmap streamed = Utils.streamIn(path, width > height ? 1 : width, height > width ? 1 : height);
                return Utils.fitInSpace(streamed, width, height);
            }
        };
    }

    public Runnable loadApproximate(final String path, final int width, final int height, ResizeCallback callback){
        return new StreamResizeRunnable(callback) {
            @Override
            public Bitmap resize() {
                return Utils.streamIn(path, width, height);
            }
        };
    }

    public Runnable loadAsIs(final InputStream is, final Bitmap recycle, final ResizeCallback callback) {
        return new StreamResizeRunnable(callback) {
            @Override
            public Bitmap resize() {
                return Utils.load(is, recycle);
            }
        };
    }

    public Runnable loadAsIs(String path, ResizeCallback callback){
        return loadAsIs(path, null, callback);
    }

    public Runnable loadAsIs(final String path, final Bitmap recycled, ResizeCallback callback){
        return new StreamResizeRunnable(callback) {
            @Override
            public Bitmap resize() {
                return Utils.load(path, recycled);
            }
        };
    };

    private abstract class StreamResizeRunnable implements Runnable {
        private final PhotoStreamResizer.ResizeCallback callback;

        public StreamResizeRunnable(PhotoStreamResizer.ResizeCallback callback) {
            this.callback = callback;
        }

        @Override
        public final void run() {
            try {
                final Bitmap result = resize();
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

        public abstract Bitmap resize();
    }
}
