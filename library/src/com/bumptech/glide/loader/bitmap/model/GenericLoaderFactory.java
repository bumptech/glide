package com.bumptech.glide.loader.bitmap.model;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintain a map of model class to factory to retrieve a {@link ModelLoaderFactory} and/or a {@link ModelLoader}
 * for a given model type.
 */
public class GenericLoaderFactory {
    private Map<Class, Map<Class, ModelLoaderFactory>> modelClassToResourceFactories =
            new HashMap<Class, Map<Class, ModelLoaderFactory>>();
    private Map<Class, Map<Class, ModelLoader>> cachedModelLoaders =
            new HashMap<Class, Map<Class, ModelLoader>>();

    @SuppressWarnings("unchecked")
    public <T, Y> ModelLoaderFactory<T, Y> register(Class<T> modelClass, Class<Y> resourceClass,
            ModelLoaderFactory<T, Y> factory) {
        Map<Class, ModelLoaderFactory> resourceToFactories = modelClassToResourceFactories.get(modelClass);
        if (resourceToFactories == null) {
            resourceToFactories = new HashMap<Class, ModelLoaderFactory>();
            modelClassToResourceFactories.put(modelClass, resourceToFactories);
        }

        ModelLoaderFactory<T, Y> previous = resourceToFactories.put(resourceClass, factory);

        if (previous != null) {
            // This factory may be being used by another model. We don't want to say it has been removed unless we
            // know it has been removed for all models.
            for (Map<Class, ModelLoaderFactory> currentResourceToFactories : modelClassToResourceFactories.values()) {
                if (currentResourceToFactories.containsValue(previous)) {
                    previous = null;
                    break;
                }
            }
        }

        cachedModelLoaders.clear();

        return previous;
    }

    public <T, Y> ModelLoader<T, Y> buildModelLoader(Class<T> modelClass, Class<Y> resourceClass, Context context) {
        ModelLoader<T, Y> result = getCachedLoader(modelClass, resourceClass);
        if (result != null) {
            return result;
        }

        final ModelLoaderFactory<T, Y> factory = getFactory(modelClass, resourceClass);
        if (factory != null) {
            result = factory.build(context, this);
            cacheModelLoader(modelClass, resourceClass, result);
        }
        return result;
    }

    private <T, Y> void cacheModelLoader(Class<T> modelClass, Class<Y> resourceClass, ModelLoader<T, Y> modelLoader) {
        Map<Class, ModelLoader> resourceToLoaders = cachedModelLoaders.get(modelClass);
        if (resourceToLoaders == null) {
            resourceToLoaders = new HashMap<Class, ModelLoader>();
            cachedModelLoaders.put(modelClass, resourceToLoaders);
        }
        resourceToLoaders.put(resourceClass, modelLoader);
    }

    @SuppressWarnings("unchecked")
    private <T, Y> ModelLoader<T, Y> getCachedLoader(Class<T> modelClass, Class<Y> resourceClass) {
        Map<Class, ModelLoader> resourceToLoaders = cachedModelLoaders.get(modelClass);
        ModelLoader result = null;
        if (resourceToLoaders != null) {
            result = resourceToLoaders.get(resourceClass);
        }

        if (result == null) {
            for (Class registeredModelClass : cachedModelLoaders.keySet()) {
                if (registeredModelClass.isAssignableFrom(modelClass)) {
                    Map<Class,  ModelLoader> currentResourceToLoaders = cachedModelLoaders.get(registeredModelClass);
                    if (currentResourceToLoaders != null) {
                        result = currentResourceToLoaders.get(resourceClass);
                        if (result != null) {
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private <T, Y> ModelLoaderFactory<T, Y> getFactory(Class<T> modelClass, Class<Y> resourceClass) {
        Map<Class, ModelLoaderFactory> resourceToFactories = modelClassToResourceFactories.get(modelClass);
        ModelLoaderFactory result = null;
        if (resourceToFactories != null) {
            result = resourceToFactories.get(resourceClass);
        }


        if (result == null) {
            for (Class registeredModelClass : modelClassToResourceFactories.keySet()) {
                // This accounts for model subclasses, our map only works for exact matches. We should however still
                // match a subclass of a model with a factory for a super class of that model if if there isn't a
                // factory for that particular subclass. Uris are a great example of when this happens, most uris
                // are actually subclasses for Uri, but we'd generally rather load them all with the same factory rather
                // than trying to register for each subclass individually.
                if (registeredModelClass.isAssignableFrom(modelClass)) {
                    Map<Class, ModelLoaderFactory> currentResourceToFactories =
                            modelClassToResourceFactories.get(registeredModelClass);
                    if (currentResourceToFactories != null) {
                        result = currentResourceToFactories.get(resourceClass);
                        if (result != null) {
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }
}
