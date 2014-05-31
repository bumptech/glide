package com.bumptech.glide.load.resource;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.Priority;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

/**
 *
 */
public abstract class LocalUriFetcher<T> implements ResourceFetcher<T> {
    private final WeakReference<Context> contextRef;
    private final Uri uri;

    /**
     * Opens an input stream for a uri pointing to a local asset. Only certain uris are supported
     *
     * @see ContentResolver#openInputStream(android.net.Uri)
     *
     * @param context A context (this will be weakly referenced and the load will fail if the weak reference
     *                is cleared before {@link #loadResource(Priority)}} is called.
     * @param uri A Uri pointing to a local asset. This load will fail if the uri isn't openable by
     *            {@link ContentResolver#openInputStream(android.net.Uri)}
     */
    public LocalUriFetcher(Context context, Uri uri) {
        contextRef = new WeakReference<Context>(context);
        this.uri = uri;
    }

    @Override
    public final T loadResource(Priority priority) throws Exception {
        Context context = contextRef.get();
        if (context == null) {
            throw new NullPointerException("Context has been cleared in LocalUriFetcher uri: " + uri);
        }
        ContentResolver contentResolver = context.getContentResolver();
        return loadResource(uri, contentResolver);
    }

    @Override
    public void cancel() { }

    protected abstract T loadResource(Uri uri, ContentResolver contentResolver) throws FileNotFoundException;
}

