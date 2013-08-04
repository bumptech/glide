package com.bumptech.glide.loader.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.stream.LocalUriLoader;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.io.File;

/**
 * A simple model loader for {@link File}
 */
public class FileLoader implements ModelLoader<File> {
    private final Context context;

    public FileLoader(Context context) {
        this.context = context;
    }

    @Override
    public StreamLoader getStreamLoader(File model, int width, int height) {
        return new LocalUriLoader(context, Uri.fromFile(model));
    }

    @Override
    public String getId(File model) {
        //canonical is better, but also slower
        return model.getAbsolutePath();
    }
}
