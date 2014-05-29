package com.bumptech.glide.load;

import com.bumptech.glide.Resource;

import java.io.OutputStream;

public interface ResourceEncoder<T> {
    public void encode(Resource<T> resource, OutputStream os);

    public String getId();
}
