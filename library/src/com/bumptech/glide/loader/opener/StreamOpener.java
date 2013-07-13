package com.bumptech.glide.loader.opener;

import java.io.IOException;
import java.io.InputStream;

/**
 * An interface that encapsulates code to open InputStreams for an image.
 */
public interface StreamOpener {
    /**
     * A method to actually create InputStreams. It will always be called on a background thread and therefore it is
     * safe to perform long running requests in this method (like an http call). This method is used to load an image
     * only if that image is not cached so this code may or may not be called
     *
     * @return A holder containing both opened InputStreams
     * @throws IOException
     */
    public InputStream openStream() throws IOException;

    /**
     * A method that is called after openStreams in a try/finally block to allow this object to cleanup anything
     * releated to the InputStreams. The InputStreams will be closed after the image load so it is not neccessary to
     * close the InputStreams here. Instead this should be used for things like
     * {@link java.net.HttpURLConnection#disconnect()} that must only be done after the streams are used.
     *
     * This method will not be called if openStreams is not called (ie if the image is cached)
     */
    public void cleanup();
}
