package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;

class EngineKeyFactory {

    @SuppressWarnings("rawtypes")
    public EngineKey buildKey(String id, Key signature, int width, int height, Transformation transformation,
            Class<?> resourceClass, Class<?> transcodeClass) {
        // TODO: what if I request a bitmap for an animated GIF, cache just the Bitmap, and then ask for the animated
        // gif?
        return new EngineKey(id, signature, width, height, transformation, resourceClass, transcodeClass);
    }

}
