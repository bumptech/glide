package com.bumptech.glide.load.model;

import android.text.TextUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This is a simple wrapper for strings representing http/https URLs. new URL() is an excessively expensive operation
 * that may be unnecessary if the class loading the image from the URL doesn't actually require a URL object.
 *
 * Users wishing to replace the class for handling URLs must register a factory using GlideUrl.
 */
public class GlideUrl {
    private String stringUrl;
    private URL url;

    public GlideUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("URL must not be null!");
        }
        this.url = url;
        stringUrl = null;
    }

    public GlideUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("String url must not be empty or null: " + url);
        }
        this.stringUrl = url;
        this.url = null;
    }

    public URL toURL() throws MalformedURLException {
        if (url == null) {
            url = new URL(stringUrl);
        }
        return url;
    }

    @Override
    public String toString() {
        if (TextUtils.isEmpty(stringUrl)) {
            stringUrl = url.toString();
        }
        return stringUrl;
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
