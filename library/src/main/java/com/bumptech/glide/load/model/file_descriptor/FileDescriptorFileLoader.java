package com.bumptech.glide.load.model.file_descriptor;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.model.FileLoader;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.File;
import java.util.List;

/**
 * A {@link ModelLoader} For translating {@link File} models into {@link ParcelFileDescriptor} data.
 */
public class FileDescriptorFileLoader extends FileLoader<ParcelFileDescriptor>
        implements FileDescriptorModelLoader<File> {

    /**
     * The default {@link com.bumptech.glide.load.model.ModelLoaderFactory} for
     * {@link com.bumptech.glide.load.model.file_descriptor.FileDescriptorFileLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<File, ParcelFileDescriptor> {

        @Override
        public ModelLoader<File, ParcelFileDescriptor> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new FileDescriptorFileLoader(multiFactory.build(Uri.class, ParcelFileDescriptor.class));
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    protected FileDescriptorFileLoader(ModelLoader<Uri, ParcelFileDescriptor> uriLoader) {
        super(uriLoader);
    }
}
