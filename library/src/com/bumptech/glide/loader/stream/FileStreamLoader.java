package com.bumptech.glide.loader.stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * A simple stream loader to retrieve an {@link java.io.InputStream} for a given path or {@link File}
 */
public class FileStreamLoader implements StreamLoader {
    private final File file;

    public FileStreamLoader(String path) {
        this(new File(path));
    }

    public FileStreamLoader(File file) {
        this.file = file;
    }

    @Override
    public void loadStream(StreamReadyCallback cb) {
        try {
            cb.onStreamReady(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            cb.onException(e);
        }
    }

    @Override
    public void cancel() { }
}
