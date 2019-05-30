package com.bumptech.glide.load.resource;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.Preconditions;

/**
 * Simple wrapper for an arbitrary object which helps to satisfy some of the glide engine's
 * contracts. <b>Suggested usages only include resource object which don't have size and cannot be
 * recycled/closed.</b>
 *
 * @param <T> type of the wrapped resource
 */
// TODO: there isn't much point in caching these...
public class SimpleResource<T> implements Resource<T> {
  protected final T data;

  public SimpleResource(@NonNull T data) {
    this.data = Preconditions.checkNotNull(data);
  }

  @NonNull
  @SuppressWarnings("unchecked")
  @Override
  public Class<T> getResourceClass() {
    return (Class<T>) data.getClass();
  }

  @NonNull
  @Override
  public final T get() {
    return data;
  }

  @Override
  public final int getSize() {
    return 1;
  }

  @Override
  public void recycle() {
    // no op
  }
}
