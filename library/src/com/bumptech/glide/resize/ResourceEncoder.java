package com.bumptech.glide.resize;

import java.io.OutputStream;

public interface ResourceEncoder<T> {
    public void encode(Resource<T> resource, OutputStream os);

    public String getId();
}
