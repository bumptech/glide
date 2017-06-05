package com.bumptech.glide.samples.flickr;

import android.content.Context;
import android.os.Environment;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.samples.flickr.api.Photo;

import java.io.File;
import java.io.InputStream;

/**
 * Register {@link FlickrModelLoader} for the Flickr sample app.
 */
@GlideModule
public class FlickrGlideModule extends AppGlideModule {
    public static final String FOLDER_NAME = "FlickrGlide";
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 250;//250 Megabytes

    @Override
    public void registerComponents(Context context, Registry registry) {
        registry.append(Photo.class, InputStream.class, new FlickrModelLoader.Factory());
        registry.register(byte[].class, new ConcealFileEncoder());
        registry.append(File.class, byte[].class, new ConcealFileDecoder());

    }

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

        if (BuildConfig.DEBUG) {
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            builder.setDiskCache(new DiskLruCacheFactory(directory.getAbsolutePath(), FOLDER_NAME, DISK_CACHE_SIZE));
        }

    }


    // Disable manifest parsing to avoid adding similar modules twice.
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
