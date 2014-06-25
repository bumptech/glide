package com.bumptech.glide.load.model.stream;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.MediaStoreThumbFetcher;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.InputStream;

public class MediaStoreStreamLoader implements ModelLoader<Uri, InputStream> {
    private final Context context;
    private final ModelLoader<Uri, InputStream> uriLoader;
    private String mimeType;
    private final long dateModified;
    private final int orientation;

    public MediaStoreStreamLoader(Context context, ModelLoader<Uri, InputStream> uriLoader, String mimeType,
            long dateModified, int orientation) {
        this.context = context;
        this.uriLoader = uriLoader;
        this.mimeType = mimeType;
        this.dateModified = dateModified;
        this.orientation = orientation;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(Uri model, int width, int height) {
        return new MediaStoreThumbFetcher(context, model, uriLoader.getResourceFetcher(model, width, height), width,
                height, mimeType, dateModified, orientation);
    }
}
