package com.bumptech.glide.resize;

import com.bumptech.glide.resize.load.Transformation;

public class EngineKeyFactory implements KeyFactory {
    @Override
    public Key buildKey(String id, int width, int height, ResourceDecoder cacheDecoder, ResourceDecoder sourceDecoder,
            Transformation transformation, ResourceEncoder encoder) {
        return new EngineKey(id, width, height, cacheDecoder, sourceDecoder, transformation, encoder);
    }
}
