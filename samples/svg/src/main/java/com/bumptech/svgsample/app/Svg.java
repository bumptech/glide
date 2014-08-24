package com.bumptech.svgsample.app;

import java.io.InputStream;

/**
 * An empty class representing an SVG file.
 */
public class Svg {

    public static Svg fromStream(InputStream is) {
        return new Svg();
    }

    public byte[] toBytes() {
        // Actually get the bytes or otherwise have some method to get at data
        // to write to cache.
        return new byte[0];
    }
}
