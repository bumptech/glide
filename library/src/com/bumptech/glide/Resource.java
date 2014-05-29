package com.bumptech.glide;

public interface Resource<T> {

    public T get();

    public int getSize();

    public void recycle();
}
