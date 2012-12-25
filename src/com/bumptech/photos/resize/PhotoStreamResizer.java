/*
 * Copyright (c) 2012 Bump Technologies Inc. All rights reserved.
 */
package com.bumptech.photos.resize;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @author sam
 *
 */
public class PhotoStreamResizer {

    private final Handler loadHandler;
    private Handler mainHandler;

    public interface ResizeCallback {
        void onResizeComplete(Bitmap resized);
        void onResizeFailed(Exception e);
    }

    public PhotoStreamResizer(Handler mainHandler, Handler loadHandler){
        this.mainHandler = mainHandler;
        this.loadHandler = loadHandler;
    }

    public Future<Bitmap> resizeCenterCrop(final String path, final int width, final int height, ResizeCallback callback){
        Callable<Bitmap> task = new Callable<Bitmap>(){
            @Override
            public Bitmap call() throws Exception {
                Bitmap result = null, streamed = null;
                streamed = Utils.streamIn(path, width, height);

                if (streamed.getWidth() == width && streamed.getHeight() == height) {
                    return streamed;
                }
                } else {
                    return Utils.centerCrop(streamed, width, height);
                }
            }
        };
        return startTask(task, callback);
    }

    public Future<Bitmap> fitInSpace(final String path, final int width, final int height, ResizeCallback callback){
        Callable<Bitmap> task = new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                final Bitmap streamed = Utils.streamIn(path, width > height ? 1 : width, height > width ? 1 : height);
                return Utils.fitInSpace(streamed, width, height);
            }
        };
        return startTask(task, callback);
    }

    public Future<Bitmap> loadApproximate(final String path, final int width, final int height, ResizeCallback callback){
        Callable<Bitmap> task = new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                return Utils.streamIn(path, width, height);
            }
        };
        return startTask(task, callback);
    }

    public Future<Bitmap> loadAsIs(final InputStream is, final Bitmap recycle, final ResizeCallback callback) {
        Callable<Bitmap> task = new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                return Utils.load(is, recycle);
            }
        };
        return startTask(task, callback);
    }

    public Future<Bitmap> loadAsIs(String path, ResizeCallback callback){
        return loadAsIs(path, null, callback);
    }

    public Future<Bitmap> loadAsIs(final String path, final Bitmap recycled, ResizeCallback callback){
        Callable<Bitmap> task  = new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                return Utils.load(path, recycled);
            }
        };
        return startTask(task, callback);
    }

    private Future<Bitmap> startTask(Callable<Bitmap> task, ResizeCallback callback){
        StreamResizeFuture future = new StreamResizeFuture(task, mainHandler, callback);
        loadHandler.post(future);
        return future;
    }

    private static class StreamResizeFuture extends FutureTask<Bitmap> {
        private final Handler mainHandler;
        private final ResizeCallback callback;
        private Bitmap result;

        public StreamResizeFuture(Callable<Bitmap> resizeTask, Handler mainHandler, ResizeCallback callback) {
            super(resizeTask);
            this.mainHandler = mainHandler;
            this.callback = callback;
        }

        @Override
        protected void done() {
            super.done();
            if (!isCancelled()){
                try {
                    result = get();
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
        }
    }
}
