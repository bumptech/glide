package com.bumptech.glide.load.data;

import androidx.annotation.NonNull;
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
        @NonNull
        @Override
        public DataRewinder<Object> build(@NonNull Object data) {
          return new DefaultRewinder(data);
        }

        @NonNull
        @Override
        public Class<Object> getDataClass() {
          throw new UnsupportedOperationException("Not implemented");
        }
      };

  public synchronized void register(@NonNull DataRewinder.Factory<?> factory) {
    rewinders.put(factory.getDataClass(), factory);
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public synchronized <T> DataRewinder<T> build(@NonNull T data) {
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

  private static final class DefaultRewinder implements DataRewinder<Object> {
    private final Object data;

    DefaultRewinder(@NonNull Object data) {
      this.data = data;
    }

    @NonNull
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
