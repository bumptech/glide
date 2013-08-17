package com.bumptech.glide.loader.model;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintain a map of model class to factory to retrieve a {@link com.bumptech.glide.loader.model.ModelLoaderFactory} and/or a {@link ModelLoader}
 * for a given model type.
 */
public class GenericLoaderFactory {
    private Map<Class, ModelLoaderFactory> factories = new HashMap<Class, ModelLoaderFactory>();

    @SuppressWarnings("unchecked")
    public <T> ModelLoaderFactory<T> register(Class<T> modelClass, ModelLoaderFactory<T> factory) {
        ModelLoaderFactory<T> previous = factories.put(modelClass, factory);
        //if factory is being used for another model class,
        //it hasn't actually been removed
        if (previous != null && factories.containsValue(previous)) {
            previous = null;
        }
        return previous;
    }

    public <T> ModelLoader<T> buildModelLoader(Class<T> modelClass, Context context) {
        final ModelLoaderFactory<T> factory = getFactory(modelClass);
        if (factory == null) {
            throw new IllegalArgumentException("No ModelLoaderFactory registered for class=" + modelClass);
        }
        return factory.build(context, this);
    }

    @SuppressWarnings("unchecked")
    public <T> ModelLoaderFactory<T> getFactory(Class<T> modelClass) {
        ModelLoaderFactory result = factories.get(modelClass);
        if (result == null) {
            for (Class registeredModelClass : factories.keySet()) {
                if (registeredModelClass.isAssignableFrom(modelClass)) {
                    result = factories.get(registeredModelClass);
                    break;
                }
            }
        }

        return result;
    }
}
