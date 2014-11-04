package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.executor.Prioritized;
import com.bumptech.glide.request.ResourceCallback;

/**
 * A runnable class responsible for using an {@link com.bumptech.glide.load.engine.DecodeJob} to decode resources on a
 * background thread in two stages.
 *
 * <p>
 *     In the first stage, this class attempts to decode a resource
 *     from cache, first using transformed data and then using source data. If no resource can be decoded from cache,
 *     this class then requests to be posted again. During the second stage this class then attempts to use the
 *     {@link com.bumptech.glide.load.engine.DecodeJob} to decode data directly from the original source.
 * </p>
 *
 * <p>
 *     Using two stages with a re-post in between allows us to run fast disk cache decodes on one thread and slow source
 *     fetches on a second pool so that loads for local data are never blocked waiting for loads for remote data to
 *     complete.
 * </p>
 */
class EngineRunnable implements Runnable, Prioritized {
    private static final String TAG = "EngineRunnable";

    private final Priority priority;
    private final EngineRunnableManager manager;
    private final DecodeJob<?, ?, ?> decodeJob;

    private Stage stage;

    private volatile boolean isCancelled;

    public EngineRunnable(EngineRunnableManager manager, DecodeJob<?, ?, ?> decodeJob, Priority priority) {
        this.manager = manager;
        this.decodeJob = decodeJob;
        this.stage = Stage.CACHE;
        this.priority = priority;
    }

    public void cancel() {
        isCancelled = true;
        decodeJob.cancel();
    }

    @Override
    public void run() {
        if (isCancelled) {
            return;
        }

        Exception exception = null;
        Resource<?> resource = null;
        try {
            resource = decode();
        } catch (Exception e) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Exception decoding", e);
            }
            exception = e;
        }

        if (isCancelled) {
            if (resource != null) {
                resource.recycle();
            }
            return;
        }

        if (resource == null) {
            onLoadFailed(exception);
        } else {
            onLoadComplete(resource);
        }
    }

    private boolean isDecodingFromCache() {
        return stage == Stage.CACHE;
    }

    private void onLoadComplete(Resource resource) {
        manager.onResourceReady(resource);
    }

    private void onLoadFailed(Exception e) {
        if (isDecodingFromCache()) {
            stage = Stage.SOURCE;
            manager.submitForSource(this);
        } else {
            manager.onException(e);
        }
    }

    private Resource<?> decode() throws Exception {
        if (isDecodingFromCache()) {
            return decodeFromCache();
        } else {
            return decodeFromSource();
        }
    }

    private Resource<?> decodeFromCache() throws Exception {
        Resource<?> result = null;
        try {
            result = decodeJob.decodeResultFromCache();
        } catch (Exception e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Exception decoding result from cache: " + e);
            }
        }

        if (result == null) {
            result = decodeJob.decodeSourceFromCache();
        }
        return result;
    }

    private Resource<?> decodeFromSource() throws Exception {
        return decodeJob.decodeFromSource();
    }

    @Override
    public int getPriority() {
        return priority.ordinal();
    }

    private enum Stage {
        /** Attempting to decode resource from cache. */
        CACHE,
        /** Attempting to decode resource from source data. */
        SOURCE
    }

    interface EngineRunnableManager extends ResourceCallback {
        void submitForSource(EngineRunnable runnable);
    }
}
