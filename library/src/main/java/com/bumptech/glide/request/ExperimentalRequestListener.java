package com.bumptech.glide.request;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.target.Target;

/**
 * An extension of {@link RequestListener} with additional parameters.
 *
 * <p>All equivalent methods are called at the relevant time by Glide. Implementations therefore
 * should only implement one version of each method.
 *
 * @param <ResourceT> The type of resource that will be loaded for the request.
 * @deprecated Not ready for public consumption, avoid using this class. It may be removed at any
 *     time.
 */
@Deprecated
public abstract class ExperimentalRequestListener<ResourceT> implements RequestListener<ResourceT> {

  /**
   * Identical to {@link #onResourceReady(Object, Object, Target, DataSource, boolean)} except that
   * {@code isAlternateCacheKey} is provided.
   *
   * @param isAlternateCacheKey True if the data was obtained from the disk cache using an alternate
   *     cache key provided by a {@link com.bumptech.glide.load.model.ModelLoader} via {@link
   *     com.bumptech.glide.load.model.ModelLoader.LoadData#alternateKeys}. Valid only if {@code
   *     dataSource} is {@link DataSource#DATA_DISK_CACHE} or {@link
   *     DataSource#RESOURCE_DISK_CACHE}.
   */
  public abstract boolean onResourceReady(
      ResourceT resource,
      Object model,
      Target<ResourceT> target,
      DataSource dataSource,
      boolean isFirstResource,
      boolean isAlternateCacheKey);
}
