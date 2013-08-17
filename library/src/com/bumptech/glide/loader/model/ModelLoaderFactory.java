package com.bumptech.glide.loader.model;

import android.content.Context;

/**
 * An interface for creating a {@link ModelLoader} for a given model type. Will be retained statically so should not
 * retain {@link Context} or any other objects that cannot be retained for the life of the application. ModelLoaders
 * will not be retained statically so it is safe for any ModelLoader built by this factory to retain a reference to a
 * {@link Context}.
 */
public interface ModelLoaderFactory<T> {
    /**
     * Build a concrete ModelLoader for this model type.
     *
     * @param context A context that cannot be retained by the factory but can be retained by the {@link ModelLoader}
     * @param factories A map of classes to factories that can be used to construct additional {@link ModelLoader}s that
     *                  this factory's {@link ModelLoader} may depend on
     * @return A new {@link ModelLoader}
     */
    public ModelLoader<T> build(Context context, GenericLoaderFactory factories);

    /**
     * Get the class of the {@link ModelLoader} this factory builds.
     *
     * @return The class of the {@link ModelLoader}
     */
    public Class<? extends ModelLoader<T>> loaderClass();

    /**
     * A lifecycle method that will be called when this factory is about to replaced
     */
    public void teardown();
}
