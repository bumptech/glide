package com.bumptech.glide.load.resource;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.load.resource.LocalUriFetcher;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class StreamLocalUriFetcher extends LocalUriFetcher<InputStream> {
    public StreamLocalUriFetcher(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected InputStream loadResource(Uri uri, ContentResolver contentResolver) throws FileNotFoundException {
        return contentResolver.openInputStream(uri);
    }
}
