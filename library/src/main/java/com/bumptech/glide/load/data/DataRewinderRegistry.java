package com.bumptech.glide.load.data;

import com.bumptech.glide.util.Preconditions;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores a mapping of data class to {@link com.bumptech.glide.load.data.DataRewinder.Factory} and
 * allows registration of new types and factories.
 */
public class DataRewinderRegistry {
  private final Map<Class<?>, DataRewinder.Factory<?>> rewinders = new HashMap<>();
  private static final DataRewinder.Factory<?> DEFAULT_FACTORY =
      new DataRewinder.Factory<Object>() {
        @Override
        public DataRewinder<Object> build(Object data) {
          return new DefaultRewinder(data);
        }

        @Override
        public Class<Object> getDataClass() {
          throw new UnsupportedOperationException("Not implemented");
        }
      };

  public synchronized void register(DataRewinder.Factory<?> factory) {
    rewinders.put(factory.getDataClass(), factory);
  }

  @SuppressWarnings("unchecked")
  public synchronized <T> DataRewinder<T> build(T data) {
    Preconditions.checkNotNull(data);
    DataRewinder.Factory<T> result = (DataRewinder.Factory<T>) rewinders.get(data.getClass());
    if (result == null) {
      for (DataRewinder.Factory<?> registeredFactory : rewinders.values()) {
        if (registeredFactory.getDataClass().isAssignableFrom(data.getClass())) {
          result = (DataRewinder.Factory<T>) registeredFactory;
          break;
        }
      }
    }

    if (result == null) {
      result = (DataRewinder.Factory<T>) DEFAULT_FACTORY;
    }
    return result.build(data);
  }

  private static class DefaultRewinder implements DataRewinder<Object> {
    private final Object data;

    public DefaultRewinder(Object data) {
      this.data = data;
    }

    @Override
    public Object rewindAndGet() {
      return data;
    }

    @Override
    public void cleanup() {
      // Do nothing.
    }
  }
}
