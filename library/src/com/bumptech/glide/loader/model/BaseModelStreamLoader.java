package com.bumptech.glide.loader.model;

import com.bumptech.glide.loader.opener.StreamOpener;

import java.lang.ref.WeakReference;

/**
 * A base class for {@link ModelStreamLoader} that provides some lifecycle methods and prevents memory leaks if a load
 * stalls or takes a long time by only providing subclasses with a weak reference to the calling
 * {@link com.bumptech.glide.presenter.ImagePresenter}.
 */
public abstract class BaseModelStreamLoader<T> implements ModelStreamLoader<T> {
    /**
     * @see ModelStreamLoader#fetchModelStream(Object, int, int, com.bumptech.glide.loader.model.ModelStreamLoader.ModelStreamReadyCallback)
     */
    @Override
    public final Object fetchModelStream(T model, int width, int height, ModelStreamReadyCallback cb) {
        doFetchModelStreams(model, width, height, new InternalModelStreamReadyCallback(cb, model));
        return cb;
    }

    /**
     * @see ModelStreamLoader#fetchModelStream(Object, int, int, com.bumptech.glide.loader.model.ModelStreamLoader.ModelStreamReadyCallback)
     */
    @Override
    public void clear() { }

    /**
     * The method where subclasses should call into whatever external class is fetching the information necessary
     * to build the id and stream opener. This method is run on the main thread so it is not safe to do long running
     * tasks. Instead pass the callback to some other class that will run on a background thread
     *
     * @param model The model representing the image
     * @param width The width of the view the image will be displayed in
     * @param height The height of the view the image will be displayed in
     * @param cb The callback to call when the id and stream opener are ready, or when the load fails
     */
    protected abstract void doFetchModelStreams(T model, int width, int height, ModelStreamReadyCallback cb);

    /**
     * A lifecycle method called after the requesting object is notifie that this loader failed to load the id and/or
     * stream opener. Should be used to cleanup or update any data related to the failed loadg.
     *
     * @param e The exception that caused the failure, or null
     * @param model The model
     * @return True iff this model stream loader has handled the exception and the cb should not be notified
     */
    protected boolean onModelStreamFetchFailed(Exception e, T model) {
        return false;
    }

    protected class InternalModelStreamReadyCallback implements ModelStreamReadyCallback {

        private final WeakReference<ModelStreamReadyCallback> cbRef;
        private final WeakReference<T> modelRef;

        public InternalModelStreamReadyCallback(ModelStreamReadyCallback cb, T model) {
            this.cbRef = new WeakReference<ModelStreamReadyCallback>(cb);
            this.modelRef = new WeakReference<T>(model);
        }

        @Override
        public boolean onStreamReady(String id, StreamOpener streamOpener) {
            ModelStreamReadyCallback cb = cbRef.get();
            boolean result = false;
            if (cb != null) {
                result = cb.onStreamReady(id, streamOpener);
            }
            return result;
        }

        @Override
        public void onException(Exception e) {
            ModelStreamReadyCallback cb = cbRef.get();
            T model = modelRef.get();
            if (cb != null && model != null) {
                if (!BaseModelStreamLoader.this.onModelStreamFetchFailed(e, model)) {
                    cb.onException(e);
                }
            }
        }
    }
}
