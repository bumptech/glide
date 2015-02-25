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
    private final String stringUrl;

    private String safeStringUrl;
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
        if (headers == null) {
            throw new IllegalArgumentException("Headers must not be null");
        }
        this.url = url;
        stringUrl = null;
        this.headers = headers;
    }

    public GlideUrl(String url, Headers headers) {
        if (TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("String url must not be empty or null: " + url);
        }
        if (headers == null) {
            throw new IllegalArgumentException("Headers must not be null");
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
        if (safeUrl == null) {
            safeUrl = new URL(getSafeStringUrl());
        }
        return safeUrl;
    }

    public String toStringUrl() {
        return getSafeStringUrl();
    }

    private String getSafeStringUrl() {
        if (TextUtils.isEmpty(safeStringUrl)) {
            String unsafeStringUrl = stringUrl;
            if (TextUtils.isEmpty(unsafeStringUrl)) {
                unsafeStringUrl = url.toString();
            }
            safeStringUrl = Uri.encode(unsafeStringUrl, ALLOWED_URI_CHARS);
        }
        return safeStringUrl;
    }

    public Map<String, String> getHeaders() {
        return headers.getHeaders();
    }

    @Override
    public String toString() {
        String urlString = getSafeStringUrl();
        StringBuilder stringBuilder = new StringBuilder(urlString);
        Map<String, String> headerMap = headers.getHeaders();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            stringBuilder.append('\n')
                .append(entry.getKey())
                .append(": ")
                .append(entry.getValue());
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GlideUrl) {
          GlideUrl other = (GlideUrl) o;
          return getSafeStringUrl().equals(other.getSafeStringUrl())
              && headers.equals(other.headers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = getSafeStringUrl().hashCode();
        hashCode = 31 * hashCode + headers.hashCode();
        return hashCode;
    }
}
