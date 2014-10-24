package com.bumptech.glide;

import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;

import java.io.File;

interface DownloadOptions {

    /**
     * Loads the original unmodified data into the cache and calls the given Target with the cache File.
     *
     * @param target The Target that will receive the cache File when the load completes
     * @param <Y> The type of Target.
     * @return The given Target.
     */
    <Y extends Target<File>> Y downloadOnly(Y target);


    /**
     * Loads the original unmodified data into the cache and returns a {@link java.util.concurrent.Future} that can be
     * used to retrieve the cache File containing the data.
     *
     * @param width The width in pixels to use to fetch the data.
     * @param height The height in pixels to use to fetch the data.
     * @return A {@link java.util.concurrent.Future} that can be used to retrieve the cache File containing the data.
     */
    FutureTarget<File> downloadOnly(int width, int height);
}
