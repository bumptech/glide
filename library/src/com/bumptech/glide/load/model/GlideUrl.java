package com.bumptech.glide.load.model;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This is a simple wrapper for strings representing http/https urls. new URL() is an excessively expensive operation
 * that may be unnecessary if the class loading the image from the url doesn't actually require a URL object.
 *
 * Users wishing to replace the class for handling urls must register a factory using GlideUrl.
 */
public class GlideUrl {
    private final String stringUrl;
    private final URL url;

    public GlideUrl(URL url) {
        this.url = url;
        stringUrl = null;
    }

    public GlideUrl(String url) {
        this.stringUrl = url;
        this.url = null;
    }

    public URL toURL() throws MalformedURLException {
        return url != null ? url : new URL(stringUrl);
    }

    @Override
    public String toString() {
        return url != null ? url.toString() : stringUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GlideUrl glideUrl = (GlideUrl) o;
        if (stringUrl != null) {
            if (glideUrl.stringUrl != null) {
                return stringUrl.equals(glideUrl.stringUrl);
            } else {
                return stringUrl.equals(glideUrl.url.toString());
            }
        } else {
            if (glideUrl.stringUrl != null) {
                return url.toString().equals(glideUrl.stringUrl);
            } else {
                return url.equals(glideUrl.url);
            }
        }
    }

    @Override
    public int hashCode() {
        if (stringUrl != null) {
            return stringUrl.hashCode();
        } else {
            return url.toString().hashCode();
        }
    }
}
