package com.bumptech.glide.resize;

import com.bumptech.glide.resize.load.Transformation;

public interface KeyFactory {

    public Key buildKey(String id, int width, int height, ResourceDecoder cacheDecoder, ResourceDecoder sourceDecoder,
            Transformation transformation, ResourceEncoder encoder);
}
