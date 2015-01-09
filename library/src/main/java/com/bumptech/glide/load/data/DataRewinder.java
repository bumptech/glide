package com.bumptech.glide.load.data;

import java.io.IOException;

/**
 * Responsible for rewinding a stream like data types.
 * @param <T> The stream like data type that can be rewound.
 */
public interface DataRewinder<T> {

    interface Factory<T> {
        DataRewinder<T> build(T data);
        Class getDataClass();
    }

    /**
     * Rewinds the wrapped data back to the position it was at when this object was instantiated and returns the
     * re-wound data (or a wrapper for the re-wound data).
     *
     * @return An object pointing to the wrapped data.
     * @throws IOException
     */
    T rewindAndGet() throws IOException;

    /**
     * Called when this rewinder is no longer needed and can be cleaned up.
     *
     * <p>
     *     The underlying data may still be in use and should not be closed or invalidated.
     * </p>
     */
    void cleanup();
}
