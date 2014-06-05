package com.bumptech.glide.load.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.bumptech.glide.Priority;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 *
 */
public abstract class LocalUriFetcher<T extends Closeable> implements DataFetcher<T> {
    private static final String TAG = "LocalUriFetcher";
    private final WeakReference<Context> contextRef;
    private final Uri uri;
    private T data;

    /**
     * Opens an input stream for a uri pointing to a local asset. Only certain uris are supported
     *
     * @see ContentResolver#openInputStream(android.net.Uri)
     *
     * @param context A context (this will be weakly referenced and the load will fail if the weak reference
     *                is cleared before {@link #loadData(Priority)}} is called.
     * @param uri A Uri pointing to a local asset. This load will fail if the uri isn't openable by
     *            {@link ContentResolver#openInputStream(android.net.Uri)}
     */
    public LocalUriFetcher(Context context, Uri uri) {
        contextRef = new WeakReference<Context>(context);
        this.uri = uri;
    }

    @Override
    public final T loadData(Priority priority) throws Exception {
        Context context = contextRef.get();
        if (context == null) {
            throw new NullPointerException("Context has been cleared in LocalUriFetcher uri: " + uri);
        }
        ContentResolver contentResolver = context.getContentResolver();
        data = loadResource(uri, contentResolver);
        return data;
    }

    @Override
    public void cleanup() {
        if (data != null) {
            try {
                data.close();
            } catch (IOException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "failed to close data", e);
                }
            }

        }
    }

    @Override
    public void cancel() {
        // Do nothing.
    }

    protected abstract T loadResource(Uri uri, ContentResolver contentResolver) throws FileNotFoundException;
}

