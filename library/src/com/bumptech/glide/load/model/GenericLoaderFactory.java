package com.bumptech.glide.load.model;

import android.content.Context;
import com.bumptech.glide.load.data.DataFetcher;

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

    private static final ModelLoader NULL_MODEL_LOADER = new ModelLoader() {
        @Override
        public DataFetcher getResourceFetcher(Object model, int width, int height) {
            throw new NoSuchMethodError("This should never be called!");
        }

        @Override
        public String getId(Object model) {
            throw new NoSuchMethodError("This should never be called!");
        }

        @Override
        public String toString() {
            return "NULL_MODEL_LOADER";
        }
    };

    /**
     * Removes and returns the registered {@link ModelLoaderFactory} for the given model and resource classes. Returns
     * null if no such factory is registered. Clears all cached model loaders.
     *
     * @param modelClass The model class.
     * @param resourceClass The resource class.
     * @param <T> The type of the model the class.
     * @param <Y> The type of the resource class.
     */
    @SuppressWarnings("unchecked")
    public <T, Y> ModelLoaderFactory<T, Y> unregister(Class<T> modelClass, Class<Y> resourceClass) {
        cachedModelLoaders.clear();

        ModelLoaderFactory<T, Y> result = null;
        Map<Class, ModelLoaderFactory> resourceToFactories = modelClassToResourceFactories.get(modelClass);
        if (resourceToFactories != null) {
            result = resourceToFactories.remove(resourceClass);
        }
        return result;
    }

    /**
     * Registers the given {@link ModelLoaderFactory} for the given model and resource classes and returns the previous
     * factory registered for the given model and resource classes or null if no such factory existed. Clears all cached
     * model loaders.
     *
     * @param modelClass The model class.
     * @param resourceClass The resource class.
     * @param factory The factory to register.
     * @param <T> The type of the model.
     * @param <Y> The type of the resource.
     */
    @SuppressWarnings("unchecked")
    public <T, Y> ModelLoaderFactory<T, Y> register(Class<T> modelClass, Class<Y> resourceClass,
            ModelLoaderFactory<T, Y> factory) {
        cachedModelLoaders.clear();

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

        return previous;
    }

    /**
     * Returns a {@link ModelLoader} for the given model and resource classes by either returning a cached
     * {@link ModelLoader} or building a new a new {@link ModelLoader} using registered {@link ModelLoaderFactory}s.
     * Returns null if no {@link ModelLoaderFactory} is registered for the given classes.
     *
     * @param modelClass The model class.
     * @param resourceClass The resource class.
     * @param <T> The type of the model.
     * @param <Y> The type of the resource.
     */
    public <T, Y> ModelLoader<T, Y> buildModelLoader(Class<T> modelClass, Class<Y> resourceClass, Context context) {
        ModelLoader<T, Y> result = getCachedLoader(modelClass, resourceClass);
        if (result != null) {
            // We've already tried to create a model loader and can't with the currently registered set of factories, but
            // we can't use null to demonstrate that failure because model loaders that haven't been requested yet will
            // be null in the cache. To avoid this, we use a special signal model loader.
            if (NULL_MODEL_LOADER.equals(result)) {
                return null;
            } else {
                return result;
            }
        }

        final ModelLoaderFactory<T, Y> factory = getFactory(modelClass, resourceClass);
        if (factory != null) {
            result = factory.build(context, this);
            cacheModelLoader(modelClass, resourceClass, result);
        } else {
            // We can't generate a model loader for the given arguments with the currently registered set of factories.
            cacheNullLoader(modelClass, resourceClass);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T, Y> void cacheNullLoader(Class<T> modelClass, Class<Y> resourceClass) {
        cacheModelLoader(modelClass, resourceClass, NULL_MODEL_LOADER);
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
