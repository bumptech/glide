package com.bumptech.photos.loader.path;

import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/1/13
 * Time: 3:04 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BasePathLoader<T> implements PathLoader<T> {
    @Override
    public final Object fetchPath(T model, int width, int height, PathReadyCallback cb) {
        doFetchPath(model, width, height, new InternalPathReadyCallback(cb, model));
        return cb;
    }

    @Override
    public void clear() { }

    protected abstract void doFetchPath(T model, int width, int height, PathReadyCallback cb);

    protected void onPathReady(String path, T model, boolean isUsed) { }

    protected void onPathFetchFailed(T model, Exception e) { }


    protected class InternalPathReadyCallback implements PathReadyCallback{
        private final WeakReference<PathReadyCallback> cbRef;
        private final WeakReference<T> modelRef;

        public InternalPathReadyCallback(PathReadyCallback cb, T model) {
            this.cbRef = new WeakReference<PathReadyCallback>(cb);
            this.modelRef = new WeakReference<T>(model);
        }

        @Override
        public final boolean onPathReady(String path) {
            final PathReadyCallback cb = cbRef.get();
            final T model = modelRef.get();
            boolean result = false;
            if (cb != null && model != null) {
                result = cb.onPathReady(path);
                BasePathLoader.this.onPathReady(path, model, result);
            }
            return result;
        }

        @Override
        public final void onError(Exception e) {
            final PathReadyCallback cb = cbRef.get();
            final T model = modelRef.get();
            if (cb != null && model != null) {
                cb.onError(e);
                BasePathLoader.this.onPathFetchFailed(model, e);
            }
        }
    }

}
