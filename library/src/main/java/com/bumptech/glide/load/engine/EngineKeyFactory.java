package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

public class EngineKeyFactory {

    public EngineKey buildKey(String id, int width, int height, ResourceDecoder cacheDecoder,
            ResourceDecoder sourceDecoder, Transformation transformation, ResourceEncoder encoder,
            ResourceTranscoder transcoder, Encoder sourceEncoder) {
        return new EngineKey(id, width, height, cacheDecoder, sourceDecoder, transformation, encoder, transcoder,
                sourceEncoder);
    }

}
