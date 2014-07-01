package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import android.os.Handler;

import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RequestFutureTarget<T, R> implements RequestListener<T, R>, FutureTarget<R>, Runnable {
    private Handler mainHandler;
    private final int width;
    private final int height;
    // exposed for testing only.
    private final boolean assertBackgroundThread;

    private R resource;
    private Request request;
    private boolean isCancelled;
    private Exception exception;
    private boolean resultReceived;

    public RequestFutureTarget(Handler mainHandler, int width, int height) {
        this(mainHandler, width, height, true);
    }

    RequestFutureTarget(Handler mainHandler, int width, int height, boolean assertBackgroundThread) {
        this.mainHandler = mainHandler;
        this.width = width;
        this.height = height;
        this.assertBackgroundThread = assertBackgroundThread;
    }

    @Override
    public synchronized boolean cancel(boolean b) {
        if (isCancelled) {
            return true;
        }

        final boolean result = !isDone();
        if (result) {
            isCancelled = true;
            clear();
        }
        notifyAll();
        return result;
    }

    @Override
    public synchronized boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public synchronized boolean isDone() {
        return isCancelled || resultReceived;
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        try {
            return doGet(null);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public R get(long time, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return doGet(timeUnit.toMillis(time));
    }

    @Override
    public synchronized boolean onResourceReady(R resource, T model, Target target,
                                   boolean isFromMemoryCache, boolean isFirstResource) {
        resultReceived = true;
        this.resource = resource;
        notifyAll();
        return true;
    }

    @Override
    public synchronized boolean onException(Exception e, Object model, Target target, boolean isFirstImage) {
        this.exception = e;
        notifyAll();
        return true;
    }

    @Override
    public void getSize(SizeReadyCallback cb) {
        cb.onSizeReady(width, height);
    }

    @Override
    public void setRequest(Request request) {
        this.request = request;
    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public void onResourceReady(R resource, GlideAnimation<R> glideAnimation) {
        // Do nothing.
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        // Do nothing.
    }

    private synchronized R doGet(Long timeoutMillis) throws ExecutionException, InterruptedException, TimeoutException {
        if (assertBackgroundThread) {
            Util.assertBackgroundThread();
        }

        if (isCancelled) {
            throw new CancellationException();
        } else if (exception != null) {
            throw new ExecutionException(exception);
        } else if (resultReceived) {
            return resource;
        }

        if (timeoutMillis == null) {
            wait(0);
        } else {
            wait(timeoutMillis);
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        } else if (exception != null) {
            throw new ExecutionException(exception);
        } else if (isCancelled) {
            throw new CancellationException();
        } else if (!resultReceived) {
            throw new TimeoutException();
        }

        return resource;
    }

    @Override
    public void run() {
        request.clear();
    }

    // All interactions with requests must be done on the main thread.
    @Override
    public void clear() {
        mainHandler.post(this);
    }
}
