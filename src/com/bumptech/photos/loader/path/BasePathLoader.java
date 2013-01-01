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
    public Object fetchPath(T model, int width, int height, PathReadyCallback cb) {
        doFetchPath(model, width, height, cb);
        return cb;
    }

    @Override
    public void clear() { }

    protected abstract void doFetchPath(T model, int width, int height, PathReadyCallback cb);

    protected void onPathReady(String path, boolean isUsed) { }

    protected void onPathFetchFailed(Exception e) { }


    protected static class InternalPathReadyCallback {
        private final WeakReference<PathReadyCallback> cbRef;
        private final WeakReference<BasePathLoader> pathLoaderRef;


        public InternalPathReadyCallback(BasePathLoader pathLoader, PathReadyCallback cb) {
            this.pathLoaderRef = new WeakReference<BasePathLoader>(pathLoader);
            this.cbRef = new WeakReference<PathReadyCallback>(cb);
        }

        protected final void onPathReady(String path) {
            final BasePathLoader pathLoader = pathLoaderRef.get();
            final PathReadyCallback cb = cbRef.get();
            if (pathLoader != null && cb != null) {
                pathLoader.onPathReady(path, cb.onPathReady(path));
            }
        }

        protected final void onError(Exception e) {
            final BasePathLoader pathLoader = pathLoaderRef.get();
            final PathReadyCallback cb = cbRef.get();
            if (pathLoader != null && cb != null) {
                cb.onError(e);
                pathLoader.onPathFetchFailed(e);
            }
        }
    }

}
