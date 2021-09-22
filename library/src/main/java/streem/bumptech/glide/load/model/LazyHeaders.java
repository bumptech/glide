package com.bumptech.glide.load.model;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A wrapper class for a set of headers to be included in a Glide request, allowing headers to be
 * constructed lazily.
 *
 * <p>Ideally headers are constructed once and then re-used for multiple loads, rather then being
 * constructed individually for each load.
 *
 * <p>This class is thread safe.
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
    Map<String, String> combinedHeaders = new HashMap<>();

    for (Map.Entry<String, List<LazyHeaderFactory>> entry : headers.entrySet()) {
      String values = buildHeaderValue(entry.getValue());
      if (!TextUtils.isEmpty(values)) {
        combinedHeaders.put(entry.getKey(), values);
      }
    }

    return combinedHeaders;
  }

  @NonNull
  private String buildHeaderValue(@NonNull List<LazyHeaderFactory> factories) {
    StringBuilder sb = new StringBuilder();
    int size = factories.size();
    for (int i = 0; i < size; i++) {
      LazyHeaderFactory factory = factories.get(i);
      String header = factory.buildHeader();
      if (!TextUtils.isEmpty(header)) {
        sb.append(header);
        if (i != factories.size() - 1) {
          sb.append(',');
        }
      }
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return "LazyHeaders{" + "headers=" + headers + '}';
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
   * Adds an {@link LazyHeaderFactory} that will be used to construct a value for the given key*
   * lazily on a background thread.
   *
   * <p>This class is not thread safe.
   *
   * <p>This class may include default values for User-Agent and Accept-Encoding headers. These will
   * be replaced by calls to either {@link #setHeader(String, LazyHeaderFactory)} or {@link
   * #addHeader(String, String)}, even though {@link #addHeader(String, LazyHeaderFactory)} would
   * usually append an additional value.
   */
  public static final class Builder {
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String DEFAULT_USER_AGENT = getSanitizedUserAgent();
    private static final Map<String, List<LazyHeaderFactory>> DEFAULT_HEADERS;

    // Set Accept-Encoding header to do our best to avoid gzip since it's both inefficient for
    // images and also makes it more difficult for us to detect and prevent partial content
    // rendering. See #440.
    static {
      Map<String, List<LazyHeaderFactory>> temp = new HashMap<>(2);
      if (!TextUtils.isEmpty(DEFAULT_USER_AGENT)) {
        temp.put(
            USER_AGENT_HEADER,
            Collections.<LazyHeaderFactory>singletonList(
                new StringHeaderFactory(DEFAULT_USER_AGENT)));
      }
      DEFAULT_HEADERS = Collections.unmodifiableMap(temp);
    }

    private boolean copyOnModify = true;
    private Map<String, List<LazyHeaderFactory>> headers = DEFAULT_HEADERS;
    private boolean isUserAgentDefault = true;

    /**
     * Adds a value for the given header and returns this builder.
     *
     * <p>Use {@link #addHeader(String, LazyHeaderFactory)} if obtaining the value requires I/O
     * (i.e. an OAuth token).
     *
     * @see #addHeader(String, LazyHeaderFactory)
     */
    public Builder addHeader(@NonNull String key, @NonNull String value) {
      return addHeader(key, new StringHeaderFactory(value));
    }

    /**
     * Adds an {@link LazyHeaderFactory} that will be used to construct a value for the given key
     * lazily on a background thread.
     *
     * <p>Headers may have multiple values whose order is defined by the order in which this method
     * is called.
     *
     * <p>This class does not prevent you from adding the same value to a given key multiple times
     */
    public Builder addHeader(@NonNull String key, @NonNull LazyHeaderFactory factory) {
      if (isUserAgentDefault && USER_AGENT_HEADER.equalsIgnoreCase(key)) {
        return setHeader(key, factory);
      }

      copyIfNecessary();
      getFactories(key).add(factory);
      return this;
    }

    /**
     * Replaces all existing {@link LazyHeaderFactory LazyHeaderFactorys} for the given key with the
     * given {@link LazyHeaderFactory}.
     *
     * <p>If the given value is {@code null}, the header at the given key will be removed.
     *
     * <p>Use {@link #setHeader(String, LazyHeaderFactory)} if obtaining the value requires I/O
     * (i.e. an OAuth token).
     */
    @SuppressWarnings({"UnusedReturnValue", "WeakerAccess"}) // Public API
    public Builder setHeader(@NonNull String key, @Nullable String value) {
      return setHeader(key, value == null ? null : new StringHeaderFactory(value));
    }

    /**
     * Replaces all existing {@link LazyHeaderFactory LazyHeaderFactorys} for the given key with the
     * given {@link LazyHeaderFactory}.
     *
     * <p>If the given value is {@code null}, the header at the given key will be removed.
     */
    public Builder setHeader(@NonNull String key, @Nullable LazyHeaderFactory factory) {
      copyIfNecessary();
      if (factory == null) {
        headers.remove(key);
      } else {
        List<LazyHeaderFactory> factories = getFactories(key);
        factories.clear();
        factories.add(factory);
      }

      if (isUserAgentDefault && USER_AGENT_HEADER.equalsIgnoreCase(key)) {
        isUserAgentDefault = false;
      }

      return this;
    }

    private List<LazyHeaderFactory> getFactories(String key) {
      List<LazyHeaderFactory> factories = headers.get(key);
      if (factories == null) {
        factories = new ArrayList<>();
        headers.put(key, factories);
      }
      return factories;
    }

    private void copyIfNecessary() {
      if (copyOnModify) {
        copyOnModify = false;
        headers = copyHeaders();
      }
    }

    /** Returns a new immutable {@link LazyHeaders} object. */
    public LazyHeaders build() {
      copyOnModify = true;
      return new LazyHeaders(headers);
    }

    private Map<String, List<LazyHeaderFactory>> copyHeaders() {
      Map<String, List<LazyHeaderFactory>> result = new HashMap<>(headers.size());
      for (Map.Entry<String, List<LazyHeaderFactory>> entry : headers.entrySet()) {
        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        List<LazyHeaderFactory> valueCopy = new ArrayList<>(entry.getValue());
        result.put(entry.getKey(), valueCopy);
      }
      return result;
    }

    /**
     * Ensures that the default header will pass OkHttp3's checks for header values.
     *
     * @see <a href="https://github.com/bumptech/glide/issues/2331">#2331</a>
     */
    @VisibleForTesting
    static String getSanitizedUserAgent() {
      String defaultUserAgent = System.getProperty("http.agent");
      if (TextUtils.isEmpty(defaultUserAgent)) {
        return defaultUserAgent;
      }

      int length = defaultUserAgent.length();
      StringBuilder sb = new StringBuilder(defaultUserAgent.length());
      for (int i = 0; i < length; i++) {
        char c = defaultUserAgent.charAt(i);
        if ((c > '\u001f' || c == '\t') && c < '\u007f') {
          sb.append(c);
        } else {
          sb.append('?');
        }
      }
      return sb.toString();
    }
  }

  static final class StringHeaderFactory implements LazyHeaderFactory {

    @NonNull private final String value;

    StringHeaderFactory(@NonNull String value) {
      this.value = value;
    }

    @Override
    public String buildHeader() {
      return value;
    }

    @Override
    public String toString() {
      return "StringHeaderFactory{" + "value='" + value + '\'' + '}';
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
