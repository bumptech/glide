package com.bumptech.glide.load.engine;

import androidx.annotation.NonNull;

/**
 * A resource interface that wraps a particular type so that it can be pooled and reused.
 *
 * @param <Z> The type of resource wrapped by this class.
 */
public interface Resource<Z> {

  /** Returns the {@link Class} of the wrapped resource. */
  @NonNull
  Class<Z> getResourceClass();

  /**
   * Returns an instance of the wrapped resource.
   *
   * <p>Note - This does not have to be the same instance of the wrapped resource class and in fact
   * it is often appropriate to return a new instance for each call. For example, {@link
   * android.graphics.drawable.Drawable Drawable}s should only be used by a single {@link
   * android.view.View View} at a time so each call to this method for Resources that wrap {@link
   * android.graphics.drawable.Drawable Drawable}s should always return a new {@link
   * android.graphics.drawable.Drawable Drawable}.
   */
  @NonNull
  Z get();

  /**
   * Returns the size in bytes of the wrapped resource to use to determine how much of the memory
   * cache this resource uses.
   */
  int getSize();

  /**
   * Cleans up and recycles internal resources.
   *
   * <p>It is only safe to call this method if there are no current resource consumers and if this
   * method has not yet been called. Typically this occurs at one of two times:
   *
   * <ul>
   *   <li>During a resource load when the resource is transformed or transcoded before any consumer
   *       have ever had access to this resource
   *   <li>After all consumers have released this resource and it has been evicted from the cache
   * </ul>
   *
   * For most users of this class, the only time this method should ever be called is during
   * transformations or transcoders, the framework will call this method when all consumers have
   * released this resource and it has been evicted from the cache.
   */
  void recycle();
}
