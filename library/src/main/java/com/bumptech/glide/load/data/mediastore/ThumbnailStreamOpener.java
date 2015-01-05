package com.bumptech.glide.load.data.mediastore;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

class ThumbnailStreamOpener {
    private static final FileService DEFAULT_SERVICE = new FileService();
    private final FileService service;
    private ThumbnailQuery query;

    public ThumbnailStreamOpener(ThumbnailQuery query) {
        this(DEFAULT_SERVICE, query);
    }

    public ThumbnailStreamOpener(FileService service, ThumbnailQuery query) {
        this.service = service;
        this.query = query;
    }

    public InputStream open(Context context, Uri uri) throws FileNotFoundException {
        Uri thumbnailUri = null;
        InputStream inputStream = null;

        final Cursor cursor = query.query(context, uri);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                String path = cursor.getString(0);
                if (!TextUtils.isEmpty(path)) {
                    File file = service.get(path);
                    if (service.exists(file) && service.length(file) > 0) {
                        thumbnailUri = Uri.fromFile(file);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (thumbnailUri != null) {
            inputStream = context.getContentResolver().openInputStream(thumbnailUri);
        }
        return inputStream;
    }
}
