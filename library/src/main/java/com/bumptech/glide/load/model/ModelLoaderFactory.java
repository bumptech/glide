package com.bumptech.glide.load.model;

import android.content.Context;

/**
 * An interface for creating a {@link ModelLoader} for a given model type. Will be retained statically so should not
 * retain {@link Context} or any other objects that cannot be retained for the life of the application. ModelLoaders
 * will not be retained statically so it is safe for any ModelLoader built by this factory to retain a reference to a
 * {@link Context}.
 *
 * @param <T> The type of the model the {@link com.bumptech.glide.load.model.ModelLoader}s built by this factory
 *           can handle
 * @param <Y> The type of data the {@link com.bumptech.glide.load.model.ModelLoader}s built by this factory can load.
 */
public interface ModelLoaderFactory<T, Y> {

    /**
     * Build a concrete ModelLoader for this model type.
     *
     * @param context A context that cannot be retained by the factory but can be retained by the {@link ModelLoader}
     * @param factories A map of classes to factories that can be used to construct additional {@link ModelLoader}s that
     *                  this factory's {@link ModelLoader} may depend on
     * @return A new {@link ModelLoader}
     */
    ModelLoader<T, Y> build(Context context, GenericLoaderFactory factories);

    /**
     * A lifecycle method that will be called when this factory is about to replaced.
     */
    void teardown();
}
