package com.bumptech.glide.load.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.util.Preconditions;
import java.util.Collections;
import java.util.List;

/**
 * A factory interface for translating an arbitrarily complex data model into a concrete data type
 * that can be used by an {@link DataFetcher} to obtain the data for a resource represented by the
 * model.
 *
 * <p>This interface has two objectives: 1. To translate a specific model into a data type that can
 * be decoded into a resource.
 *
 * <p>2. To allow a model to be combined with the dimensions of the view to fetch a resource of a
 * specific size.
 *
 * <p>This not only avoids having to duplicate dimensions in xml and in your code in order to
 * determine the size of a view on devices with different densities, but also allows you to use
 * layout weights or otherwise programmatically put the dimensions of the view without forcing you
 * to fetch a generic resource size.
 *
 * <p>The smaller the resource you fetch, the less bandwidth and battery life you use, and the lower
 * your memory footprint per resource.
 *
 * @param <Model> The type of the model.
 * @param <Data> The type of the data that can be used by a {@link
 *     com.bumptech.glide.load.ResourceDecoder} to decode a resource.
 */
public interface ModelLoader<Model, Data> {

  /**
   * Contains a set of {@link com.bumptech.glide.load.Key Keys} identifying the source of the load,
   * alternate cache keys pointing to equivalent data, and a {@link
   * com.bumptech.glide.load.data.DataFetcher} that can be used to fetch data not found in cache.
   *
   * @param <Data> The type of data that well be loaded.
   */
  class LoadData<Data> {
    public final Key sourceKey;
    public final List<Key> alternateKeys;
    public final DataFetcher<Data> fetcher;

    public LoadData(@NonNull Key sourceKey, @NonNull DataFetcher<Data> fetcher) {
      this(sourceKey, Collections.<Key>emptyList(), fetcher);
    }

    public LoadData(
        @NonNull Key sourceKey,
        @NonNull List<Key> alternateKeys,
        @NonNull DataFetcher<Data> fetcher) {
      this.sourceKey = Preconditions.checkNotNull(sourceKey);
      this.alternateKeys = Preconditions.checkNotNull(alternateKeys);
      this.fetcher = Preconditions.checkNotNull(fetcher);
    }
  }

  /**
   * Returns a {@link com.bumptech.glide.load.model.ModelLoader.LoadData} containing a {@link
   * com.bumptech.glide.load.data.DataFetcher} required to decode the resource represented by this
   * model, as well as a set of {@link com.bumptech.glide.load.Key Keys} that identify the data
   * loaded by the {@link com.bumptech.glide.load.data.DataFetcher} as well as an optional list of
   * alternate keys from which equivalent data can be loaded. The {@link DataFetcher} will not be
   * used if the resource is already cached.
   *
   * <p>Note - If no valid data fetcher can be returned (for example if a model has a null URL),
   * then it is acceptable to return a null data fetcher from this method.
   *
   * @param model The model representing the resource.
   * @param width The width in pixels of the view or target the resource will be loaded into, or
   *     {@link com.bumptech.glide.request.target.Target#SIZE_ORIGINAL} to indicate that the
   *     resource should be loaded at its original width.
   * @param height The height in pixels of the view or target the resource will be loaded into, or
   *     {@link com.bumptech.glide.request.target.Target#SIZE_ORIGINAL} to indicate that the
   *     resource should be loaded at its original height.
   */
  @Nullable
  LoadData<Data> buildLoadData(
      @NonNull Model model, int width, int height, @NonNull Options options);

  /**
   * Returns true if the given model is a of a recognized type that this loader can probably load.
   *
   * <p>For example, you may want multiple Uri -> InputStream loaders. One might handle media store
   * Uris, another might handle asset Uris, and a third might handle file Uris etc.
   *
   * <p>This method is generally expected to do no I/O and complete quickly, so best effort results
   * are acceptable. {@link ModelLoader ModelLoaders} that return true from this method may return
   * {@code null} from {@link #buildLoadData(Object, int, int, Options)}
   */
  boolean handles(@NonNull Model model);
}
