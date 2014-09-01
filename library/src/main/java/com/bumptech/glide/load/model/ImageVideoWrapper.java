package com.bumptech.glide.load.model;

import android.os.ParcelFileDescriptor;

import java.io.InputStream;

/**
 * A simple wrapper that wraps an {@link java.io.InputStream} and/or an {@link android.os.ParcelFileDescriptor}.
 */
public class ImageVideoWrapper {
    private final InputStream streamData;
    private final ParcelFileDescriptor fileDescriptor;

    public ImageVideoWrapper(InputStream streamData, ParcelFileDescriptor fileDescriptor) {
        this.streamData = streamData;
        this.fileDescriptor = fileDescriptor;
    }

    public InputStream getStream() {
        return streamData;
    }

    public ParcelFileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }
}
