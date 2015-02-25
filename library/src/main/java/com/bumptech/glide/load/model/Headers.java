package com.bumptech.glide.load.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper class for a set of headers to be included in a Glide request.
 */
public class Headers {

    public static final Headers NONE = new Builder().build();

    private final Map<String, Set<String>> headers;
    private volatile Map<String, String> combinedHeaders;

    private Headers(Map<String, Set<String>> headers) {
        this.headers = Collections.unmodifiableMap(headers);
    }

    public Map<String, String> getHeaders() {
        if (combinedHeaders == null) {
            synchronized (this) {
                if (combinedHeaders == null) {
                    Map<String, String> combinedHeaders = new HashMap<>();

                    for (String key : headers.keySet()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (String value : headers.get(key)) {
                            stringBuilder.append(",").append(value);
                        }
                        if (stringBuilder.length() > 0) {
                            stringBuilder.deleteCharAt(0);
                            combinedHeaders.put(key, stringBuilder.toString());
                        }
                    }

                    this.combinedHeaders = Collections.unmodifiableMap(combinedHeaders);
                }
            }
        }

        return combinedHeaders;
    }

  /**
   * Builder class for {@link Headers}.
   */
    public static class Builder {
        private final Map<String, Set<String>> headers = new HashMap<>();

        public void addHeader(String key, String value) {
            if (headers.containsKey(key)) {
                headers.get(key).add(value);
            } else {
                Set<String> values = new HashSet<>();
                values.add(value);
                headers.put(key, values);
            }
        }

        public Headers build() {
            return new Headers(headers);
        }
    }
}
