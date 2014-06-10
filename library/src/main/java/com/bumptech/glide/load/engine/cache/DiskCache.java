package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * An interface for writing to and reading from a disk cache
 */
public interface DiskCache {
    /**
     * An interface to actually write data to a key in
     * the disk cache
     */
    public interface Writer {
        /**
         * Writes data to the output stream and returns true if the write was successful and should be committed, and
         * false if the write should be aborted.
         *
         * @param os The output stream the Writer should write to.
         */
        public boolean write(OutputStream os);
    }

    /**
     * Get an InputStream for the value at the given key.
     *
     * @param key The key in the cache
     * @return An InputStream representing the data at key at the time get is called
     */
    public InputStream get(Key key);

    /**
     * Write to a key in the cache. {@link Writer} is used so that the cache implementation
     * can perform actions after the write finishes, like commit (via atomic file rename).
     *
     * @param key The key to write to
     * @param writer An interface that will write data given an OutputStream for the key
     */
    public void put(Key key, Writer writer);

    /**
     * Remove the key and value from the cache
     *
     * @param key The key to remove
     */
    public void delete(Key key);
}
