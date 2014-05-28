package com.bumptech.glide.resize;

import com.bumptech.glide.loader.bitmap.model.ModelLoader;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RequestContext {

    private final RequestContext parent;
    private Map<MultiClassKey, Object> dependencies;

    public RequestContext(RequestContext parent) {
        this.parent = parent;
    }

    public RequestContext() {
        this.parent = null;
    }

    public <A, T, Z> boolean canLoad(Class<A> modelClass, Class<T> sourceClass, Class<Z> resourceClass) {
        boolean hasModelLoader = contains(new MultiClassKey(ModelLoader.class, modelClass, sourceClass));
        boolean hasDecoder = contains(new MultiClassKey(ResourceDecoder.class, sourceClass, resourceClass));
        boolean hasEncoder = contains(new MultiClassKey(ResourceEncoder.class, resourceClass));
        boolean hasCacheDecoder = contains(new MultiClassKey(ResourceDecoder.class, InputStream.class, resourceClass));

        return hasModelLoader && hasDecoder && hasEncoder && hasCacheDecoder;
    }

    public <A, T> void register(ModelLoader<A, T> modelLoader, Class<A> modelClass, Class<T> sourceClass) {
        registerGeneric(modelLoader, ModelLoader.class, modelClass, sourceClass);
    }

    public <Z> void register(ResourceEncoder<Z> encoder, Class<Z> clazz) {
        registerGeneric(encoder, ResourceEncoder.class, clazz);
    }

    public <T, Z> void register(ResourceDecoder<T, Z> decoder, Class<T> sourceClazz, Class<Z> decodedClazz) {
        registerGeneric(decoder, ResourceDecoder.class, sourceClazz, decodedClazz);
    }

    public <T> void register(ResourceDecoder<InputStream, T> cacheDecoder, Class<T> clazz) {
        register(cacheDecoder, InputStream.class, clazz);
    }

    public <A, T> ModelLoader<A, T> getModelLoader(Class<A> modelClass, Class<T> sourceClass) {
        return (ModelLoader<A, T>) getGeneric(ModelLoader.class, modelClass, sourceClass);
    }

    public <Z> ResourceEncoder<Z> getEncoder(Class<Z> clazz) {
        return (ResourceEncoder<Z>) getGeneric(ResourceEncoder.class, clazz);
    }

    public <T> ResourceDecoder<InputStream, T> getCacheDecoder(Class <T> clazz) {
        return getDecoder(InputStream.class, clazz);
    }

    public <T, Z> ResourceDecoder<T, Z> getDecoder(Class<T> sourceClazz, Class<Z> decodedClass) {
        return (ResourceDecoder<T, Z>) getGeneric(ResourceDecoder.class, sourceClazz, decodedClass);
    }

    void registerGeneric(Object object, Class... classes) {
        if (object == null) {
            throw new NullPointerException("Cannot register a null object");
        }
        getDependencies().put(new MultiClassKey(classes), object);
    }

    Object getGeneric(Class... classes) {
        Object result = null;
        if (dependencies != null) {
            result = dependencies.get(new MultiClassKey(classes));
        }
        if (result == null) {
            if (parent != null) {
                result = parent.getGeneric(classes);
            } else {
                throw new DependencyNotFoundException(classes);
            }
        }
        return result;
    }

    private boolean contains(MultiClassKey key) {
        boolean contains = false;
        if (dependencies != null) {
            contains = dependencies.containsKey(key);
        }
        if (!contains && parent != null) {
            contains = parent.contains(key);
        }
        return contains;
    }

    private Map<MultiClassKey, Object> getDependencies() {
        if (dependencies == null) {
            dependencies = new HashMap<MultiClassKey, Object>();
        }
        return dependencies;
    }

    public static class DependencyNotFoundException extends RuntimeException {
        public DependencyNotFoundException(Class... types) {
            super("Failed to find " + types[0] + " with types " + getClassTypesString(types));
        }

        private static String getClassTypesString(Class[] types) {
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < types.length; i++) {
                Class type = types[i];
                builder.append(type.getName()).append(", ");
            }
            return types.length > 1 ? builder.substring(0, builder.length() - 2) : "";
        }
    }

    private static class MultiClassKey {
        private final Class[] classes;

        public MultiClassKey(Class... classes) {
            this.classes = classes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MultiClassKey that = (MultiClassKey) o;

            if (!Arrays.equals(classes, that.classes)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(classes);
        }
    }
}
