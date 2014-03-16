package com.bumptech.glide.loader.bitmap.model.file_descriptor;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.loader.bitmap.model.GenericLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.ModelLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.UriLoader;
import com.bumptech.glide.loader.bitmap.resource.FileDescriptorLocalUriFetcher;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.net.URL;

/**
 * A {@link ModelLoader} For translating {@link Uri} models for local uris into {@link ParcelFileDescriptor} resources.
 */
public class FileDescriptorUriLoader extends UriLoader<ParcelFileDescriptor> implements FileDescriptorModelLoader<Uri> {
    public static class Factory implements ModelLoaderFactory<Uri, ParcelFileDescriptor> {
        @Override
        public ModelLoader<Uri, ParcelFileDescriptor> build(Context context, GenericLoaderFactory factories) {
            return new FileDescriptorUriLoader(context, factories.buildModelLoader(URL.class, ParcelFileDescriptor.class,
                    context));
        }

        @Override
        public Class<? extends ModelLoader<Uri, ParcelFileDescriptor>> loaderClass() {
            return FileDescriptorUriLoader.class;
        }

        @Override
        public void teardown() { }
    }

    public FileDescriptorUriLoader(Context context, ModelLoader<URL, ParcelFileDescriptor> urlLoader) {
        super(context, urlLoader);
    }

    @Override
    protected ResourceFetcher<ParcelFileDescriptor> getLocalUriFetcher(Context context, Uri uri) {
        return new FileDescriptorLocalUriFetcher(context, uri);
    }
}
