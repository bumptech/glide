package com.bumptech.glide.loader.stream;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

/**
 *
 */
public class LocalUriLoader implements StreamLoader {
    private final WeakReference<Context> contextRef;
    private final Uri uri;

    /**
     * Opens an input stream for a uri pointing to a local asset. Only certain uris are supported
     *
     * @see ContentResolver#openInputStream(android.net.Uri)
     *
     * @param context A context (this will be weakly referenced and the load will silently abort if the weak reference
     *                is cleared before {@link #loadStream(StreamReadyCallback cb)} is called
     * @param uri A Uri pointing to a local asset. This load will fail if the uri isn't openable by
     *            {@link ContentResolver#openInputStream(android.net.Uri)}
     */
    public LocalUriLoader(Context context, Uri uri) {
        contextRef = new WeakReference<Context>(context);
        this.uri = uri;
    }

    @Override
    public void loadStream(StreamReadyCallback cb) {
        final Context context = contextRef.get();
        if (context != null) {
            final ContentResolver contentResolver = context.getContentResolver();
            try {
                cb.onStreamReady(contentResolver.openInputStream(uri));
            } catch (FileNotFoundException e) {
                cb.onException(e);
            }
        }

    }

    @Override
    public void cancel() { }
}

