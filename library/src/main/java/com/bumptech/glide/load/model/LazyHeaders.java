package com.bumptech.glide.load.model;

import android.text.TextUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper class for a set of headers to be included in a Glide request, allowing headers to be
 * constructed lazily.
 *
 * <p> Should be used instead of BasicHeaders when constructing headers requires I/O. </p>
 */
public final class LazyHeaders implements Headers {
    private final Map<String, Set<String>> eagerHeaders;
    private final Map<String, Set<LazyHeaderFactory>> lazyHeaders;
    private volatile Map<String, String> combinedHeaders;

    LazyHeaders(Map<String, Set<String>> eagerHeaders,
        Map<String, Set<LazyHeaderFactory>> lazyHeaders) {
        this.eagerHeaders = Collections.unmodifiableMap(eagerHeaders);
        this.lazyHeaders = Collections.unmodifiableMap(lazyHeaders);
    }

    @Override
    public Map<String, String> getHeaders() {
        if (combinedHeaders == null) {
            synchronized (this) {
                if (combinedHeaders == null) {
                    this.combinedHeaders = Collections.unmodifiableMap(generateHeaders());
                }
            }
        }

        return combinedHeaders;
    }

    private Map<String, String> generateHeaders() {
        Map<String, String> combinedHeaders = new HashMap<String, String>();
        Set<String> combinedKeys = new HashSet<String>(eagerHeaders.keySet());
        combinedKeys.addAll(lazyHeaders.keySet());

        for (String key : combinedKeys) {
            Set<String> values = new HashSet<String>();
            if (eagerHeaders.containsKey(key)) {
                values.addAll(eagerHeaders.get(key));
            }
            if (lazyHeaders.containsKey(key)) {
                for (LazyHeaderFactory factory : lazyHeaders.get(key)) {
                    values.add(factory.buildHeader());
                }
            }
            combinedHeaders.put(key, TextUtils.join(",", values));
        }

        return combinedHeaders;
    }

    @Override
    public String toString() {
        Set<String> combinedKeys = new HashSet<String>(eagerHeaders.keySet());
        combinedKeys.addAll(lazyHeaders.keySet());

        StringBuilder stringBuilder = new StringBuilder();
        for (String key : combinedKeys) {
            stringBuilder.append(key)
                .append(": ");
            if (eagerHeaders.containsKey(key)) {
                stringBuilder.append(TextUtils.join(",", eagerHeaders.get(key)));
            }
            if (lazyHeaders.containsKey(key)) {
                for (LazyHeaderFactory factory : lazyHeaders.get(key)) {
                    stringBuilder.append(factory.toString());
                    stringBuilder.append(',');
                }
            }
            stringBuilder.append('\n');
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

        LazyHeaders otherHeaders = (LazyHeaders) o;
        return eagerHeaders.equals(otherHeaders.eagerHeaders)
            && lazyHeaders.equals(otherHeaders.lazyHeaders);
    }

    @Override
    public int hashCode() {
        return eagerHeaders.hashCode() + 31 * lazyHeaders.hashCode();
    }

    /**
     * Builder class for {@link BasicHeaders}.
     */
    public static final class Builder {
        private final Map<String, Set<String>> eagerHeaders = new HashMap<String, Set<String>>();
        private final Map<String, Set<LazyHeaderFactory>> lazyHeaders =
            new HashMap<String, Set<LazyHeaderFactory>>();

        public void addHeader(String key, String value) {
            Set<String> values = eagerHeaders.get(key);
            if (values == null) {
                values = new HashSet<String>();
                eagerHeaders.put(key, values);
            }
            values.add(value);
        }

        public void addHeader(String key, LazyHeaderFactory factory) {
            Set<LazyHeaderFactory> factories = lazyHeaders.get(key);
            if (factories == null) {
                factories = new HashSet<LazyHeaderFactory>();
                lazyHeaders.put(key, factories);
            }
            factories.add(factory);
        }

        public LazyHeaders build() {
          return new LazyHeaders(eagerHeaders, lazyHeaders);
        }
    }
}
