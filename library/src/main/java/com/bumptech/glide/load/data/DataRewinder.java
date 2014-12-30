package com.bumptech.glide.load.data;

import java.io.IOException;

public interface DataRewinder<T> {

    interface Factory<T> {
        DataRewinder<T> build(T data);
        Class getDataClass();
    }

    T rewindAndGet() throws IOException;

    void cleanup();
}
