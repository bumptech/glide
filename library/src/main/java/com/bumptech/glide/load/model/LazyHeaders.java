package com.bumptech.glide.load.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A wrapper class for a set of headers to be included in a Glide request, allowing headers to be
 * constructed lazily.
 *
 * <p> Ideally headers are constructed once and then re-used for multiple loads, rather then being
 * constructed individually for each load. </p>
 *
 * <p> This class is thread safe. </p>
 */
public final class LazyHeaders implements Headers {
    private final Map<String, List<LazyHeaderFactory>> headers;
    private volatile Map<String, String> combinedHeaders;

    LazyHeaders(Map<String, List<LazyHeaderFactory>> headers) {
        this.headers = Collections.unmodifiableMap(headers);
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

        for (Map.Entry<String, List<LazyHeaderFactory>> entry : headers.entrySet()) {
            StringBuilder sb = new StringBuilder();
            List<LazyHeaderFactory> factories = entry.getValue();
            for (int i = 0; i < factories.size(); i++) {
                LazyHeaderFactory factory = factories.get(i);
                sb.append(factory.buildHeader());
                if (i != factories.size() - 1) {
                    sb.append(',');
                }
            }
            combinedHeaders.put(entry.getKey(), sb.toString());
        }

        return combinedHeaders;
    }

    @Override
    public String toString() {
        return "LazyHeaders{"
            + "headers=" + headers
            + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LazyHeaders) {
            LazyHeaders other = (LazyHeaders) o;
            return headers.equals(other.headers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return headers.hashCode();
    }

    /**
     * Builder class for {@link LazyHeaders}.
     *
     * <p> This class is not thread safe. </p>
     */
    public static final class Builder {
        private boolean copyOnModify;
        private Map<String, List<LazyHeaderFactory>> headers =
            new HashMap<String, List<LazyHeaderFactory>>();

        /**
         * Adds a value for the given header and returns this builder.
         *
         * <p> Use {@link #addHeader(String, LazyHeaderFactory)} if obtaining the value requires I/O
         * (ie an oauth token). </p>
         *
         * @see #addHeader(String, LazyHeaderFactory)

         */
        public Builder addHeader(String key, String value) {
            return addHeader(key, new StringHeaderFactory(value));
        }

        /**
         * Adds an {@link LazyHeaderFactory} that will be used to construct a value for the given
         * key lazily on a background thread.
         *
         * <p> Headers may have multiple values whose order is defined by the order in which
         * this method is called. </p>
         *
         * <p> This class does not prevent you from adding the same value to a given key multiple
         * times </p>
         */
        public Builder addHeader(String key, LazyHeaderFactory factory) {
            if (copyOnModify) {
                copyOnModify = false;
                headers = copyHeaders();
            }

            List<LazyHeaderFactory> factories = headers.get(key);
            if (factories == null) {
                factories = new ArrayList<LazyHeaderFactory>();
                headers.put(key, factories);
            }
            factories.add(factory);
            return this;
        }

        /**
         * Returns a new immutable {@link LazyHeaders} object.
         */
        public LazyHeaders build() {
          copyOnModify = true;
          return new LazyHeaders(headers);
        }

        private Map<String, List<LazyHeaderFactory>> copyHeaders() {
            Map<String, List<LazyHeaderFactory>> result =
                  new HashMap<String, List<LazyHeaderFactory>>(headers.size());
            for (Map.Entry<String, List<LazyHeaderFactory>> entry : headers.entrySet()) {
                result.put(entry.getKey(), new ArrayList<LazyHeaderFactory>(entry.getValue()));
            }
            return result;
        }
    }

    static final class StringHeaderFactory implements LazyHeaderFactory {

        private final String value;

        StringHeaderFactory(String value) {
            this.value = value;
        }

        @Override
        public String buildHeader() {
            return value;
        }

        @Override
        public String toString() {
            return "StringHeaderFactory{"
                + "value='" + value + '\''
                + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof StringHeaderFactory) {
                StringHeaderFactory other = (StringHeaderFactory) o;
                return value.equals(other.value);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
