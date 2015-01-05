package com.bumptech.glide.load.model.file_descriptor;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.model.StringLoader;

import java.util.List;

/**
 * A {@link ModelLoader} For translating {@link String} models, such as file paths, into {@link ParcelFileDescriptor}
 * data.
 */
public class FileDescriptorStringLoader extends StringLoader<ParcelFileDescriptor>
        implements FileDescriptorModelLoader<String> {

    /**
     * The default factory for {@link com.bumptech.glide.load.model.file_descriptor.FileDescriptorStringLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<String, ParcelFileDescriptor> {

        @Override
        public ModelLoader<String, ParcelFileDescriptor> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new FileDescriptorStringLoader(multiFactory.build(Uri.class, ParcelFileDescriptor.class));
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    protected FileDescriptorStringLoader(ModelLoader<Uri, ParcelFileDescriptor> uriLoader) {
        super(uriLoader);
    }
}
