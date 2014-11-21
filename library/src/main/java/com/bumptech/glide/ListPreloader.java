package com.bumptech.glide;

import android.widget.AbsListView;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.util.Util;

import java.util.List;
import java.util.Queue;

/**
 * Loads a few resources ahead in the direction of scrolling in any {@link AbsListView} so that images are in the memory
 * cache just before the corresponding view in created in the list. Gives the appearance of an infinitely large image
 * cache, depending on scrolling speed, cpu speed, and cache size.
 *
 * <p>
 * Must be set using {@link AbsListView#setOnScrollListener(android.widget.AbsListView.OnScrollListener)}, or have its
 * corresponding methods called from another {@link android.widget.AbsListView.OnScrollListener} to function.
 * </p>
 *
 * @param <T> The type of the model being displayed in the list.
 */
public class ListPreloader<T> implements AbsListView.OnScrollListener {

    private final int maxPreload;
    private final PreloadTargetQueue preloadTargetQueue;
    private final PreloadModelProvider<T> preloadModelProvider;
    private final PreloadSizeProvider<T> preloadDimensionProvider;

    private int lastEnd;
    private int lastStart;
    private int lastFirstVisible;
    private int totalItemCount;

    private boolean isIncreasing = true;

    /**
     * An implementation of PreloadModelProvider should provide all the models that should be preloaded.
     *
     * @param <U> The type of the model being preloaded.
     */
    public interface PreloadModelProvider<U> {

        /**
         * Returns a non null list of all models that need to be loaded for the list to display adapter items in
         * positions between {@code start} and {@code end}.
         *
         * <p>
         * A list of any size can be returned so there can be multiple models per adapter position.
         * </p>
         *
         * @param position The adapter position.
         */
        List<U> getPreloadItems(int position);

        /**
         * Returns a non null {@link com.bumptech.glide.GenericRequestBuilder} for a given item. Must exactly match
         * the request used to load the resource in the list.
         *
         * <p>
         * The target and context will be provided by the preloader.
         * </p>
         *
         * @param item The model to load.
         */
        GenericRequestBuilder getPreloadRequestBuilder(U item);
    }

    /**
     * An implementation of PreloadSizeProvider should provide the size of the view in the list where the resources
     * will be displayed.
     *
     * @param <T> The type of the model the size should be provided for.
     */
    public interface PreloadSizeProvider<T> {

        /**
         * Returns the size of the view in the list where the resources will be displayed in pixels in the format
         * [x, y], or {@code null} if no size is currently available.
         *
         * <p>
         * Note - The dimensions returned here must precisely match those of the view in the list.
         * </p>
         *
         * @param item A model
         */
        int[] getPreloadSize(T item, int adapterPosition, int perItemPosition);
    }

    /**
     * Constructor for {@link com.bumptech.glide.ListPreloader} that requires users to subclass and override
     * the {@link #getItems(int, int)} and {@link #getRequestBuilder(Object)} methods.
     *
     * @deprecated Use {@link #ListPreloader(com.bumptech.glide.ListPreloader.PreloadModelProvider,
     * com.bumptech.glide.ListPreloader.PreloadSizeProvider, int)} instead. This constructor will be removed in Glide
     * 4.0.
     * @param maxPreload Maximum number of items to preload.
     */
    @Deprecated
    public ListPreloader(int maxPreload) {
        this.preloadModelProvider = new PreloadModelProvider<T>() {
            @Override
            public List<T> getPreloadItems(int position) {
                return getItems(position, position + 1);
            }

            @Override
            public GenericRequestBuilder getPreloadRequestBuilder(T item) {
                return getRequestBuilder(item);
            }
        };
        this.preloadDimensionProvider = new PreloadSizeProvider<T>() {

            @Override
            public int[] getPreloadSize(T item, int adapterPosition, int perItemPosition) {
                return getDimensions(item);
            }
        };
        this.maxPreload = maxPreload;
        preloadTargetQueue = new PreloadTargetQueue(maxPreload + 1);

    }

    /**
     * Constructor for {@link com.bumptech.glide.ListPreloader} that accepts interfaces for providing the dimensions of
     * images to preload, the list of models to preload for a given position, and the request to use to load images.
     *
     * @param preloadModelProvider     Provides models to load and requests capable of loading them.
     * @param preloadDimensionProvider Provides the dimensions of images to load.
     * @param maxPreload               Maximum number of items to preload.
     */
    public ListPreloader(PreloadModelProvider<T> preloadModelProvider,
                         PreloadSizeProvider<T> preloadDimensionProvider, int maxPreload) {
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
    public void onScroll(AbsListView absListView, int firstVisible, int visibleCount,
                         int totalCount) {
        totalItemCount = totalCount;
        if (firstVisible > lastFirstVisible) {
            preload(firstVisible + visibleCount, true);
        } else if (firstVisible < lastFirstVisible) {
            preload(firstVisible, false);
        }
        lastFirstVisible = firstVisible;
    }

    /**
     * Returns the size of the view in the list where the resources will be displayed.
     *
     * <p>
     * Note - The size returned here must precisely match those of the view in the list.
     * </p>
     *
     * @deprecated Use {@link com.bumptech.glide.ListPreloader.PreloadSizeProvider} instead. This method will be removed
     * in Glide 4.0.
     * @param item A model
     * @return The size of the view where the item will be displayed
     */
    @Deprecated
    protected int[] getDimensions(T item) {
        throw new IllegalStateException("You must either provide a PreloadDimensionProvider or override "
                                         + "getDimensions()");
    }

    /**
     * Returns a non null list of all models that need to be loaded for the list to display adapter items
     * between {@code start} and {@code end}.
     *
     * <p>
     * A list of any size can be returned so there can be multiple models per adapter position.
     * </p>
     *
     * @deprecated Use {@link com.bumptech.glide.ListPreloader.PreloadModelProvider} instead. This method will be
     * removed in Glide 4.0.
     * @param start The smallest adapter position. Will be {@code >= 0 && < adapter.getCount() &&
     *              <= end}
     * @param end   The largest adapter position. Will be {@code >= 0 && < adapter.getCount && >=
     *              start}
     */
    @Deprecated
    protected List<T> getItems(int start, int end) {
        throw new IllegalStateException("You must either provide a PreloadModelProvider or override getItems()");
    }

    /**
     * Returns a non null {@link com.bumptech.glide.GenericRequestBuilder} for a given item. Must exactly match the
     * request used to load the resource in the list.
     *
     * <p>
     * The target and context will be provided by the preloader.
     * </p>
     *
     * @deprecated Use {@link com.bumptech.glide.ListPreloader.PreloadModelProvider} instead. This method will be
     * removed in Glide 4.0.
     * @param item The model to load.
     */
    @SuppressWarnings("rawtypes")
    @Deprecated
    protected GenericRequestBuilder getRequestBuilder(T item) {
        throw new IllegalStateException("You must either provide a PreloadModelProvider, or override "
                                         + "getRequestBuilder()");
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
                preloadAdapterPosition(this.preloadModelProvider.getPreloadItems(i), i, true);
            }
        } else {
            // Decreasing
            for (int i = end - 1; i >= start; i--) {
                preloadAdapterPosition(this.preloadModelProvider.getPreloadItems(i), i, false);
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
    private void preloadItem(T item, int position, int i) {
        final int[] dimensions = this.preloadDimensionProvider.getPreloadSize(item, position, i);
        if (dimensions != null) {
            GenericRequestBuilder preloadRequestBuilder = this.preloadModelProvider.getPreloadRequestBuilder(item);
            preloadRequestBuilder.into(preloadTargetQueue.next(dimensions[0], dimensions[1]));
        }
    }

    private void cancelAll() {
        for (int i = 0; i < maxPreload; i++) {
            Glide.clear(preloadTargetQueue.next(0, 0));
        }
    }

    private static final class PreloadTargetQueue {
        private final Queue<PreloadTarget> queue;

        public PreloadTargetQueue(int size) {
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

    private static class PreloadTarget extends BaseTarget<Object> {
        private int photoHeight;
        private int photoWidth;

        @Override
        public void onResourceReady(Object resource,
                                    GlideAnimation<? super Object> glideAnimation) {
            // Do nothing.
        }

        @Override
        public void getSize(SizeReadyCallback cb) {
            cb.onSizeReady(photoWidth, photoHeight);
        }
    }
}
