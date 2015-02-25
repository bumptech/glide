package com.bumptech.glide.load.model;

import android.net.Uri;
import android.text.TextUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * A wrapper for strings representing http/https URLs responsible for ensuring URLs are properly escaped and avoiding
 * unnecessary URL instantiations for loaders that require only string urls rather than URL objects.
 *
 * <p>
 *  Users wishing to replace the class for handling URLs must register a factory using GlideUrl.
 * </p>
 *
 * <p>
 *     To obtain a properly escaped URL, call {@link #toURL()}. To obtain a properly escaped string URL, call
 *     {@link #toURL()} and then {@link java.net.URL#toString()}.
 * </p>
 */
public class GlideUrl {
    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";

    private final URL url;
    private final Headers headers;
    private String stringUrl;

    private URL safeUrl;

    public GlideUrl(URL url) {
        this(url, Headers.NONE);
    }

    public GlideUrl(String url) {
        this(url, Headers.NONE);
    }

    public GlideUrl(URL url, Headers headers) {
        if (url == null) {
            throw new IllegalArgumentException("URL must not be null!");
        }
        this.url = url;
        stringUrl = null;
        this.headers = headers;
    }

    public GlideUrl(String url, Headers headers) {
        if (TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("String url must not be empty or null: " + url);
        }
        this.stringUrl = url;
        this.url = null;
        this.headers = headers;
    }

    public URL toURL() throws MalformedURLException {
        return getSafeUrl();
    }

    // See http://stackoverflow.com/questions/3286067/url-encoding-in-android. Although the answer using URI would work,
    // using it would require both decoding and encoding each string which is more complicated, slower and generates
    // more objects than the solution below. See also issue #133.
    private URL getSafeUrl() throws MalformedURLException {
        if (safeUrl != null) {
            return safeUrl;
        }

        safeUrl = new URL(getSafeStringUrl());
        return safeUrl;
    }

    public String toStringUrl() {
        return getSafeStringUrl();
    }

    private String getSafeStringUrl() {
        if (TextUtils.isEmpty(stringUrl)) {
            stringUrl = url.toString();
        }
        return Uri.encode(stringUrl, ALLOWED_URI_CHARS);
    }

    public Map<String, String> getHeaders() {
        return headers.getHeaders();
    }

    @Override
    public String toString() {
        String urlString = getSafeStringUrl();
        StringBuilder stringBuilder = new StringBuilder(urlString);
        Map<String, String> headerMap = headers.getHeaders();
        for (String key : headerMap.keySet()) {
            stringBuilder.append("\n")
                .append(key)
                .append(": ")
                .append(headerMap.get(key));
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
