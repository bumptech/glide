package com.bumptech.glide.load.model.file_descriptor;

import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.File;

/**
 * A base class for {@link ModelLoader}s that translate models into {@link File}s.
 *
 * @param <T> The type of the model that will be translated into an {@link File}.
 */
public interface FileDescriptorModelLoader<T> extends ModelLoader<T, ParcelFileDescriptor> {
}
