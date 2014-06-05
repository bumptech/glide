package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

public interface KeyFactory {

    public Key buildKey(String id, int width, int height, ResourceDecoder cacheDecoder, ResourceDecoder sourceDecoder,
            Transformation transformation, ResourceEncoder encoder, ResourceTranscoder transcoder);
}
