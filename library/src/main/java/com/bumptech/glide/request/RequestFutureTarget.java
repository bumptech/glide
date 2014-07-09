package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import android.os.Handler;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link java.util.concurrent.Future} implementation for Glide that can be used to load resources in a blocking
 * manner on background threads.
 *
 * <p>
 *     Note - Unlike most targets, RequestFutureTargets can be used once and only once. Attempting to reuse a
 *     RequestFutureTarget will probably result in undesirable behavior or exceptions. Instead of reusing
 *     objects of this class, the pattern should be:
 *
 *     <pre>
 *     {@code
 *      RequestFutureTarget target = Glide.load("")...
 *     Object resource = target.get();
 *     // Do something with resource, and when finished:
 *     Glide.clear(target);
 *     }
 *     </pre>
 *     The {@link com.bumptech.glide.Glide#clear(FutureTarget)} call will make sure any resources used are recycled.
 * </p>
 *
 * @param <T> The type of the data to load.
 * @param <R> The type of the resource that will be loaded.
 */
public class RequestFutureTarget<T, R> implements RequestListener<T, R>, FutureTarget<R>, Runnable {
    private final Handler mainHandler;
    private final int width;
    private final int height;
    // Exists for testing only.
    private final boolean assertBackgroundThread;

    private R resource;
    private Request request;
    private boolean isCancelled;
    private Exception exception;
    private boolean resultReceived;
    private boolean exceptionReceived;

    /**
     * Constructor for a RequestFutureTarget. Should not be used directly.
     */
    public RequestFutureTarget(Handler mainHandler, int width, int height) {
        this(mainHandler, width, height, true);
    }

    RequestFutureTarget(Handler mainHandler, int width, int height, boolean assertBackgroundThread) {
        this.mainHandler = mainHandler;
        this.width = width;
        this.height = height;
        this.assertBackgroundThread = assertBackgroundThread;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean cancel(boolean b) {
        if (isCancelled) {
            return true;
        }

        final boolean result = !isDone();
        if (result) {
            isCancelled = true;
            clear();
            notifyAll();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isCancelled() {
        return isCancelled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isDone() {
        return isCancelled || resultReceived;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public R get() throws InterruptedException, ExecutionException {
        try {
            return doGet(null);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public R get(long time, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return doGet(timeUnit.toMillis(time));
    }

    /**
     * A callback that should never be invoked directly.
     */
    @Override
    public synchronized boolean onResourceReady(R resource, T model, Target target,
                                   boolean isFromMemoryCache, boolean isFirstResource) {
        // We might get a null result.
        resultReceived = true;
        this.resource = resource;
        notifyAll();
        return true;
    }

    /**
     * A callback that should never be invoked directly.
     */
    @Override
    public synchronized boolean onException(Exception e, Object model, Target target, boolean isFirstResource) {
        // We might get a null exception.
        exceptionReceived = true;
        this.exception = e;
        notifyAll();
        return true;
    }

    /**
     * A callback that should never be invoked directly.
     */
    @Override
    public void getSize(SizeReadyCallback cb) {
        cb.onSizeReady(width, height);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRequest(Request request) {
        this.request = request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Request getRequest() {
        return request;
    }

    /**
     * A callback that should never be invoked directly.
     */
    @Override
    public final void onResourceReady(R resource, GlideAnimation<R> glideAnimation) {
        // Do nothing.
    }

    /**
     * A callback that should never be invoked directly.
     */
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
        } else if (exceptionReceived) {
            throw new ExecutionException(exception);
        } else if (resultReceived) {
            return resource;
        }

        if (timeoutMillis == null) {
            wait(0);
        } else if (timeoutMillis > 0) {
            wait(timeoutMillis);
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        } else if (exceptionReceived) {
            throw new ExecutionException(exception);
        } else if (isCancelled) {
            throw new CancellationException();
        } else if (!resultReceived) {
            throw new TimeoutException();
        }

        return resource;
    }

    /**
     * A callback that should never be invoked directly.
     */
    @Override
    public void run() {
        request.clear();
    }

    /**
     * Can be safely called from either the main thread or a background thread to cleanup the resources used by this
     * target.
     */
    @Override
    public void clear() {
        mainHandler.post(this);
    }
}
