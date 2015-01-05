package com.bumptech.glide.load.model.file_descriptor;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.model.ResourceLoader;

/**
 * A {@link ModelLoader} For translating android resource id models into {@link ParcelFileDescriptor} data.
 */
public class FileDescriptorResourceLoader extends ResourceLoader<ParcelFileDescriptor>
        implements FileDescriptorModelLoader<Integer> {

    /**
     * The default factory for {@link com.bumptech.glide.load.model.file_descriptor.FileDescriptorResourceLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<Integer, ParcelFileDescriptor> {

        @Override
        public ModelLoader<Integer, ParcelFileDescriptor> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new FileDescriptorResourceLoader(context, multiFactory.build(Uri.class, ParcelFileDescriptor.class));
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    protected FileDescriptorResourceLoader(Context context, ModelLoader<Uri, ParcelFileDescriptor> uriLoader) {
        super(context, uriLoader);
    }
}
