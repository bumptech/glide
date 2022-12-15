package com.bumptech.glide;

import android.graphics.drawable.Drawable;
import android.widget.AbsListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import java.util.List;
import java.util.Queue;

/**
 * Loads a few resources ahead in the direction of scrolling in any {@link AbsListView} so that
 * images are in the memory cache just before the corresponding view in created in the list. Gives
 * the appearance of an infinitely large image cache, depending on scrolling speed, cpu speed, and
 * cache size.
 *
 * <p>Must be put using {@link
 * AbsListView#setOnScrollListener(android.widget.AbsListView.OnScrollListener)}, or have its
 * corresponding methods called from another {@link android.widget.AbsListView.OnScrollListener} to
 * function.
 *
 * @param <T> The type of the model being displayed in the list.
 */
public class ListPreloader<T> implements AbsListView.OnScrollListener {
  private final int maxPreload;
  private final PreloadTargetQueue preloadTargetQueue;
  private final RequestManager requestManager;
  private final PreloadModelProvider<T> preloadModelProvider;
  private final PreloadSizeProvider<T> preloadDimensionProvider;

  private int lastEnd;
  private int lastStart;
  private int lastFirstVisible = -1;
  private int totalItemCount;

  private boolean isIncreasing = true;

  /**
   * An implementation of PreloadModelProvider should provide all the models that should be
   * preloaded.
   *
   * @param <U> The type of the model being preloaded.
   */
  public interface PreloadModelProvider<U> {

    /**
     * Returns a {@link List} of models that need to be loaded for the list to display adapter items
     * in positions between {@code start} and {@code end}.
     *
     * <p>{@code position} is the position in the view. If the view contains a mix of types (e.g.
     * headers and images) then not every view position will actually have any model to return here.
     * If that's the case for the given {@code position}, then return an empty list.
     *
     * <p>A list of any size can be returned so there can be multiple models per adapter position.
     *
     * <p>Every model returned by this method is expected to produce a valid {@link RequestBuilder}
     * in {@link #getPreloadRequestBuilder(Object)}. If that's not possible for any set of models,
     * avoid including them in the {@link List} returned by this method.
     *
     * <p>Although it's acceptable for the returned {@link List} to contain {@code null} models,
     * it's best to filter them from the list instead of adding {@code null} to avoid unnecessary
     * logic and expanding the size of the {@link List}
     *
     * @param position The adapter position.
     */
    @NonNull
    List<U> getPreloadItems(int position);

    /**
     * Returns a {@link RequestBuilder} for a given item on which {@link
     * RequestBuilder#load(Object)}} has been called or {@code null} if no valid load can be
     * started.
     *
     * <p>For the preloader to be effective, the {@link RequestBuilder} returned here must use
     * exactly the same size and set of options as the {@link RequestBuilder} used when the ``View``
     * is bound. You may need to specify a size in both places to ensure that the width and height
     * match exactly. If so, you can use {@link
     * com.bumptech.glide.request.RequestOptions#override(int, int)} to do so.
     *
     * <p>The target and context will be provided by the preloader.
     *
     * <p>If {@link RequestBuilder#load(Object)} is not called by this method, the preloader will
     * trigger a {@link RuntimeException}. If you don't want to load a particular item or position,
     * filter it from the list returned by {@link #getPreloadItems(int)}.
     *
     * @param item The model to load.
     */
    @Nullable
    RequestBuilder<?> getPreloadRequestBuilder(@NonNull U item);
  }

  /**
   * An implementation of PreloadSizeProvider should provide the size of the view in the list where
   * the resources will be displayed.
   *
   * @param <T> The type of the model the size should be provided for.
   */
  public interface PreloadSizeProvider<T> {

    /**
     * Returns the size of the view in the list where the resources will be displayed in pixels in
     * the format [x, y], or {@code null} if no size is currently available.
     *
     * <p>Note - The dimensions returned here must precisely match those of the view in the list.
     *
     * <p>If this method returns {@code null}, then no request will be started for the given item.
     *
     * @param item A model
     */
    @Nullable
    int[] getPreloadSize(@NonNull T item, int adapterPosition, int perItemPosition);
  }

  /**
   * Constructor for {@link com.bumptech.glide.ListPreloader} that accepts interfaces for providing
   * the dimensions of images to preload, the list of models to preload for a given position, and
   * the request to use to load images.
   *
   * @param preloadModelProvider Provides models to load and requests capable of loading them.
   * @param preloadDimensionProvider Provides the dimensions of images to load.
   * @param maxPreload Maximum number of items to preload.
   */
  public ListPreloader(
      @NonNull RequestManager requestManager,
      @NonNull PreloadModelProvider<T> preloadModelProvider,
      @NonNull PreloadSizeProvider<T> preloadDimensionProvider,
      int maxPreload) {
    this.requestManager = requestManager;
    this.preloadModelProvider = preloadModelProvider;
    this.preloadDimensionProvider = preloadDimensionProvider;
    this.maxPreload = maxPreload;
    preloadTargetQueue = new PreloadTargetQueue(maxPreload + 1);
  }

  @Override
  public void onScrollStateChanged(AbsListView absListView, int scrollState) {
    // Do nothing.
  }

  @Override
  public void onScroll(
      AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {
    if (totalItemCount == 0 && totalCount == 0) {
      return;
    }
    totalItemCount = totalCount;
    if (firstVisible > lastFirstVisible) {
      preload(firstVisible + visibleCount, true);
    } else if (firstVisible < lastFirstVisible) {
      preload(firstVisible, false);
    }
    lastFirstVisible = firstVisible;
  }

  private void preload(int start, boolean increasing) {
    if (isIncreasing != increasing) {
      isIncreasing = increasing;
      cancelAll();
    }
    preload(start, start + (increasing ? maxPreload : -maxPreload));
  }

  private void preload(int from, int to) {
    int start;
    int end;
    if (from < to) {
      start = Math.max(lastEnd, from);
      end = to;
    } else {
      start = to;
      end = Math.min(lastStart, from);
    }
    end = Math.min(totalItemCount, end);
    start = Math.min(totalItemCount, Math.max(0, start));

    if (from < to) {
      // Increasing
      for (int i = start; i < end; i++) {
        preloadAdapterPosition(
            preloadModelProvider.getPreloadItems(i), /* position= */ i, /* isIncreasing= */ true);
      }
    } else {
      // Decreasing
      for (int i = end - 1; i >= start; i--) {
        preloadAdapterPosition(
            preloadModelProvider.getPreloadItems(i), /* position= */ i, /* isIncreasing= */ false);
      }
    }

    lastStart = start;
    lastEnd = end;
  }

  private void preloadAdapterPosition(List<T> items, int position, boolean isIncreasing) {
    final int numItems = items.size();
    if (isIncreasing) {
      for (int i = 0; i < numItems; ++i) {
        preloadItem(items.get(i), position, i);
      }
    } else {
      for (int i = numItems - 1; i >= 0; --i) {
        preloadItem(items.get(i), position, i);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void preloadItem(@Nullable T item, int position, int perItemPosition) {
    if (item == null) {
      return;
    }
    int[] dimensions = preloadDimensionProvider.getPreloadSize(item, position, perItemPosition);
    if (dimensions == null) {
      return;
    }
    RequestBuilder<Object> preloadRequestBuilder =
        (RequestBuilder<Object>) preloadModelProvider.getPreloadRequestBuilder(item);
    if (preloadRequestBuilder == null) {
      return;
    }

    preloadRequestBuilder.into(preloadTargetQueue.next(dimensions[0], dimensions[1]));
  }

  private void cancelAll() {
    for (int i = 0; i < preloadTargetQueue.queue.size(); i++) {
      requestManager.clear(preloadTargetQueue.next(0, 0));
    }
  }

  private static final class PreloadTargetQueue {
    @Synthetic final Queue<PreloadTarget> queue;

    // The loop is short and the only point is to create the objects.
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    PreloadTargetQueue(int size) {
      queue = Util.createQueue(size);

      for (int i = 0; i < size; i++) {
        queue.offer(new PreloadTarget());
      }
    }

    public PreloadTarget next(int width, int height) {
      final PreloadTarget result = queue.poll();
      queue.offer(result);
      result.photoWidth = width;
      result.photoHeight = height;
      return result;
    }
  }

  private static final class PreloadTarget implements Target<Object> {
    @Synthetic int photoHeight;
    @Synthetic int photoWidth;
    @Nullable private Request request;

    @Synthetic
    PreloadTarget() {}

    @Override
    public void onLoadStarted(@Nullable Drawable placeholder) {
      // Do nothing.
    }

    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
      // Do nothing.
    }

    @Override
    public void onResourceReady(
        @NonNull Object resource, @Nullable Transition<? super Object> transition) {
      // Do nothing.
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
      // Do nothing.
    }

    @Override
    public void getSize(@NonNull SizeReadyCallback cb) {
      cb.onSizeReady(photoWidth, photoHeight);
    }

    @Override
    public void removeCallback(@NonNull SizeReadyCallback cb) {
      // Do nothing because we don't retain references to SizeReadyCallbacks.
    }

    @Override
    public void setRequest(@Nullable Request request) {
      this.request = request;
    }

    @Nullable
    @Override
    public Request getRequest() {
      return request;
    }

    @Override
    public void onStart() {
      // Do nothing.
    }

    @Override
    public void onStop() {
      // Do nothing.
    }

    @Override
    public void onDestroy() {
      // Do nothing.
    }
  }
}
