package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;

import java.io.OutputStream;

public interface ResourceEncoder<T> {
    public boolean encode(Resource<T> resource, OutputStream os);

    public String getId();
}
