package com.bumptech.glide.load.model.file_descriptor;

import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.model.ModelLoader;

/**
 * A base class for {@link ModelLoader}s that translate models into {@link java.io.File}s.
 *
 * @param <T> The type of the model that will be translated into an {@link java.io.File}.
 */
public interface FileDescriptorModelLoader<T> extends ModelLoader<T, ParcelFileDescriptor> {
    // specializing the generic arguments
}
