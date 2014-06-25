package com.bumptech.glide.load;

import java.io.OutputStream;

public interface Encoder<T> {

    public boolean encode(T data, OutputStream os);

    public String getId();
}
