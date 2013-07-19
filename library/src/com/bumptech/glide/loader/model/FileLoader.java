package com.bumptech.glide.loader.model;

import com.bumptech.glide.loader.stream.FileStreamLoader;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.io.File;

/**
 * A simple model loader for {@link File}
 */
public class FileLoader implements ModelLoader<File> {

    @Override
    public StreamLoader getStreamOpener(File model, int width, int height) {
        return new FileStreamLoader(model);
    }

    @Override
    public String getId(File model) {
        //canonical is better, but also slower
        return model.getAbsolutePath();
    }

    @Override
    public void clear() { }
}
