package com.bumptech.glide.load.resource.file;

import com.bumptech.glide.load.engine.Resource;

import java.io.File;

/**
 * A simple {@link com.bumptech.glide.load.engine.Resource} that wraps a {@link File}.
 */
public class FileResource implements Resource<File> {
    private File file;

    public FileResource(File file) {
        this.file = file;
    }

    @Override
    public File get() {
        return file;
    }

    // TODO: there isn't much point in caching these...
    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public void recycle() {

    }
}
