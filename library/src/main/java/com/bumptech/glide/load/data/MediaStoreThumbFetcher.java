package com.bumptech.glide.load.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import com.bumptech.glide.Priority;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MediaStoreThumbFetcher implements DataFetcher<InputStream> {
    private static final int MINI_WIDTH = 512;
    private static final int MINI_HEIGHT = 384;
    private static final ThumbnailStreamOpenerFactory DEFAULT_FACTORY = new ThumbnailStreamOpenerFactory();

    private final Context context;
    private final Uri mediaStoreUri;
    private final DataFetcher<InputStream> defaultFetcher;
    private final int width;
    private final int height;
    private final long dateModified;
    private final int orientation;
    private final ThumbnailStreamOpenerFactory factory;
    private InputStream inputStream;
    private String mimeType;

    public MediaStoreThumbFetcher(Context context, Uri mediaStoreUri, DataFetcher<InputStream> defaultFetcher,
            int width, int height, String mimeType, long dateModified, int orientation) {
        this(context, mediaStoreUri, defaultFetcher, width, height, mimeType, dateModified, orientation,
                DEFAULT_FACTORY);
    }

    MediaStoreThumbFetcher(Context context, Uri mediaStoreUri, DataFetcher<InputStream> defaultFetcher, int width,
            int height, String mimeType, long dateModified, int orientation, ThumbnailStreamOpenerFactory factory) {
        this.context = context;
        this.mediaStoreUri = mediaStoreUri;
        this.defaultFetcher = defaultFetcher;
        this.width = width;
        this.height = height;
        this.mimeType = mimeType;
        this.dateModified = dateModified;
        this.orientation = orientation;
        this.factory = factory;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        ThumbnailStreamOpener fetcher = factory.build(mediaStoreUri, width, height);

        if (fetcher != null) {
            inputStream = fetcher.open(context, mediaStoreUri);
        }

        if (inputStream != null) {
            return inputStream;
        } else {
            return defaultFetcher.loadData(priority);
        }
    }

    @Override
    public void cleanup() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Do nothing.
            }
        }
        defaultFetcher.cleanup();
    }

    @Override
    public String getId() {
        return mediaStoreUri + mimeType + String.valueOf(dateModified) + String.valueOf(orientation);
    }

    @Override
    public void cancel() {
        // Do nothing.
    }

    private static boolean isMediaStoreUri(Uri uri) {
        return uri != null
                && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())
                && MediaStore.AUTHORITY.equals(uri.getAuthority());
    }

    private static boolean isMediaStoreVideo(Uri uri) {
        return isMediaStoreUri(uri) && uri.getPathSegments().contains("video");
    }

    static class FileService {
        public boolean exists(File file) {
            return file.exists();
        }

        public long length(File file) {
            return file.length();
        }

        public File get(String path) {
            return new File(path);
        }
    }

    interface ThumbnailQuery {
        public Cursor query(Context context, Uri uri);
    }

    static class ThumbnailStreamOpener {
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

    static class ImageThumbnailQuery implements ThumbnailQuery {

        @Override
        public Cursor query(Context context, Uri uri) {
            String id = uri.getLastPathSegment();
            return context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, new String[] {
                            MediaStore.Images.Thumbnails.DATA
                    }, MediaStore.Images.Thumbnails.IMAGE_ID + " = ? AND " + MediaStore.Images.Thumbnails.KIND + " = ?",
                    new String[] { id, String.valueOf(MediaStore.Images.Thumbnails.MINI_KIND) }, null);
        }
    }

    static class VideoThumbnailQuery implements ThumbnailQuery {

        @Override
        public Cursor query(Context context, Uri uri) {
            String id = uri.getLastPathSegment();
            return context.getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, new String[] {
                    MediaStore.Video.Thumbnails.DATA
            }, MediaStore.Video.Thumbnails.VIDEO_ID + " = ? AND " + MediaStore.Video.Thumbnails.KIND + " = ?",
                    new String[] { id, String.valueOf(MediaStore.Video.Thumbnails.MINI_KIND) }, null);
        }
    }

    static class ThumbnailStreamOpenerFactory {

        public ThumbnailStreamOpener build(Uri uri, int width, int height) {
            if (!isMediaStoreUri(uri) || width > MINI_WIDTH || height > MINI_HEIGHT) {
                return null;
            } else if (isMediaStoreVideo(uri)) {
                return new ThumbnailStreamOpener(new VideoThumbnailQuery());
            } else {
                return new ThumbnailStreamOpener(new ImageThumbnailQuery());
            }
        }
    }
}
