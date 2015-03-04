package com.bumptech.glide.load.model;

import android.text.TextUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper class for a set of headers to be included in a Glide request.
 */
public final class BasicHeaders implements Headers {

    private final Map<String, Set<String>> headers;
    private volatile Map<String, String> combinedHeaders;

    BasicHeaders(Map<String, Set<String>> headers) {
        this.headers = Collections.unmodifiableMap(headers);
    }

    public Map<String, String> getHeaders() {
        if (combinedHeaders == null) {
            synchronized (this) {
                if (combinedHeaders == null) {
                    this.combinedHeaders = generateCombinedHeaders();
                }
            }
        }

        return combinedHeaders;
    }

    private Map<String, String> generateCombinedHeaders() {
        Map<String, String> combinedHeaders = new HashMap<String, String>();
        for (Map.Entry<String, Set<String>> entry : headers.entrySet()) {
            combinedHeaders.put(entry.getKey(), TextUtils.join(",", entry.getValue()));
        }
        return Collections.unmodifiableMap(combinedHeaders);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BasicHeaders) {
            BasicHeaders other = (BasicHeaders) o;
            return headers.equals(other.headers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return headers.hashCode();
    }

    /**
     * Builder class for {@link Headers}.
     */
    public static final class Builder {
        private final Map<String, Set<String>> headers = new HashMap<String, Set<String>>();

        public void addHeader(String key, String value) {
            if (headers.containsKey(key)) {
                headers.get(key).add(value);
            } else {
                Set<String> values = new HashSet<String>();
                values.add(value);
                headers.put(key, values);
            }
        }

        public BasicHeaders build() {
            return new BasicHeaders(headers);
        }
    }
}