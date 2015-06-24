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
 * <p>  Users wishing to replace the class for handling URLs must register a factory using GlideUrl. </p>
 *
 * <p> To obtain a properly escaped URL, call {@link #toURL()}. To obtain a properly escaped string URL, call
 * {@link #toStringUrl()}. To obtain a less safe, but less expensive to calculate cache key, call
 * {@link #getCacheKey()}. </p>
 *
 * <p> This class can also optionally wrap {@link com.bumptech.glide.load.model.Headers} for convenience. </p>
 */
public class GlideUrl {
    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";

    private final URL url;
    private final Headers headers;
    private final String stringUrl;

    private String safeStringUrl;
    private URL safeUrl;

    public GlideUrl(URL url) {
        this(url, Headers.DEFAULT);
    }

    public GlideUrl(String url) {
        this(url, Headers.DEFAULT);
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

    /**
     * Returns a properly escaped {@link java.net.URL} that can be used to make http/https requests.
     *
     * @see #toStringUrl()
     * @see #getCacheKey()
     * @throws MalformedURLException
     */
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

    /**
     * Returns a properly escaped {@link String} url that can be used to make http/https requests.
     *
     * @see #toURL()
     * @see #getCacheKey()
     */
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

    /**
     * Returns a non-null {@link Map} containing headers.
     */
    public Map<String, String> getHeaders() {
        return headers.getHeaders();
    }

    /**
     * Returns an inexpensive to calculate {@link String} suitable for use as a disk cache key.
     *
     * <p> This method does not include headers. </p>
     *
     * <p> Unlike {@link #toStringUrl()}} and {@link #toURL()}, this method does not escape input. </p>
     */
    public String getCacheKey() {
      return stringUrl != null ? stringUrl : url.toString();
    }

    @Override
    public String toString() {
        return getCacheKey() + '\n' + headers.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GlideUrl) {
          GlideUrl other = (GlideUrl) o;
          return getCacheKey().equals(other.getCacheKey())
              && headers.equals(other.headers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = getCacheKey().hashCode();
        hashCode = 31 * hashCode + headers.hashCode();
        return hashCode;
    }
}
