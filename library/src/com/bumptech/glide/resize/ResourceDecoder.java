package com.bumptech.glide.resize;

public interface ResourceDecoder<T> {

    public Resource decode(T source);
}
