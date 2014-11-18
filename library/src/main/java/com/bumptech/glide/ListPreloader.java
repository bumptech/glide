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
 * <p>
 * Must be set using {@link AbsListView#setOnScrollListener(android.widget.AbsListView.OnScrollListener)}, or have its
 * corresponding methods called from another {@link android.widget.AbsListView.OnScrollListener} to function.
 * </p>
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
     * @param <U> The type of the model being preloaded.
     */
    public interface PreloadModelProvider<U> {

        /**
         * Returns a list of all models that need to be loaded for the list to display adapter items
         * {@code start - end}.
         * A list of any size can be returned so there can be multiple models per adapter position.
         * @param start The smallest adapter position. Will be {@code >= 0 && < adapter.getCount() &&
         *              <= end}
         * @param end   The largest adapter position. Will be {@code >= 0 && < adapter.getCount && >=
         *              start}
         * @return A non null list of all models for adapter positions between {@code start} and
         * {@code end}.
         */
        List<U> getPreloadItems(int start, int end);

        /**
         * Returns a glide request for a given item. Must exactly match the request used to load the
         * resource in the list.
         * The target and context will be provided by the preloader.
         * @param item The model to load.
         * @return A non null {@link BitmapRequestBuilder}.
         */
        GenericRequestBuilder getPreloadRequestBuilder(U item);
    }

    /**
     * An implementation of PreloadSizeProvider should provide the size of the view in the list where the resources
     * will be displayed.
     * @param <T> The type of the model the size should be provided for.
     */
    public interface PreloadSizeProvider<T> {

        /**
         * Returns the size of the view in the list where the resources will be displayed.
         * <p>
         * Note - The dimensions returned here must precisely match those of the view in the list.
         * </p>
         * @param item A model
         * @return The dimensions of the view where the item will be displayed
         */
        int[] getPreloadSize(T item);
    }


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
     * <p>
     * Note - The size returned here must precisely match those of the view in the list.
     * </p>
     * @param item A model
     * @return The size of the view where the item will be displayed
     */
    protected int[] getSize(T item) {
        return this.preloadDimensionProvider.getPreloadSize(item);
    }

    /**
     * Returns a list of all models that need to be loaded for the list to display adapter items
     * {@code start - end}.
     * A list of any size can be returned so there can be multiple models per adapter position.
     * @param start The smallest adapter position. Will be {@code >= 0 && < adapter.getCount() &&
     *              <= end}
     * @param end   The largest adapter position. Will be {@code >= 0 && < adapter.getCount && >=
     *              start}
     * @return A non null list of all models for adapter positions between {@code start} and
     * {@code end}.
     */
    protected List<T> getItems(int start, int end) {
        return this.preloadModelProvider.getPreloadItems(start, end);
    }

    /**
     * Returns a glide request for a given item. Must exactly match the request used to load the
     * resource in the list.
     * The target and context will be provided by the preloader.
     * @param item The model to load.
     * @return A non null {@link BitmapRequestBuilder}.
     */
    @SuppressWarnings("rawtypes")
    protected GenericRequestBuilder getRequestBuilder(T item) {
        return this.preloadModelProvider.getPreloadRequestBuilder(item);
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
        List<T> items = getItems(start, end);

        if (from < to) {
            // Increasing
            final int numItems = items.size();
            for (int i = 0; i < numItems; i++) {
                preloadItem(items, i);
            }
        } else {
            // Decreasing
            for (int i = items.size() - 1; i >= 0; i--) {
                preloadItem(items, i);
            }
        }

        lastStart = start;
        lastEnd = end;
    }

    @SuppressWarnings("unchecked")
    private void preloadItem(List<T> items, int position) {
        final T item = items.get(position);
        final int[] dimensions = getSize(item);
        if (dimensions != null) {
            getRequestBuilder(item).into(preloadTargetQueue.next(dimensions[0], dimensions[1]));
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
