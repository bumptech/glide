package com.bumptech.glide;

import android.widget.AbsListView;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.util.Util;

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
         * Returns a models that need to be loaded to display adapter item.
         * @param position The position of the model in the adapter
         * @return A nun null model that need to be loaded to display adapter item
         */
        U getPreloadItem(int position);

        /**
         * Returns a glide request for a given item. Must exactly match the request used to load the
         * resource in the list.
         * The target and context will be provided by the preloader.
         * @param item     The model to load.
         * @param position The position of the model in the adapter
         * @return A non null {@link BitmapRequestBuilder}.
         */
        GenericRequestBuilder getPreloadRequestBuilder(U item, int position);
    }

    /**
     * @param <T>
     */
    public interface PreloadSizeProvider<T> {

        /**
         * Returns the size of the view in the list where the resources will be displayed.
         * <p>
         * Note - The dimensions returned here must precisely match those of the view in the list.
         * </p>
         * @param item     A model
         * @param position The position of the model in the adapter
         * @return The dimensions of the view where the item will be displayed
         */
        int[] getPreloadSize(T item, int position);
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
     * Returns the dimensions of the view in the list where the resources will be displayed.
     * <p>
     * Note - The dimensions returned here must precisely match those of the view in the list.
     * </p>
     * @param item     A model
     * @param position of the model
     * @return The dimensions of the view where the item will be displayed
     */
    protected int[] getDimensions(T item, int position) {
        return this.preloadDimensionProvider.getPreloadSize(item, position);
    }

    /**
     * Returns a models that need to be loaded to display adapter item.
     * @param position The position of the model in the adapter
     * @return A nun null model that need to be loaded to display adapter item
     */
    protected T getItem(int position) {
        return this.preloadModelProvider.getPreloadItem(position);
    }

    /**
     * Returns a glide request for a given item. Must exactly match the request used to load the
     * resource in the list.
     * The target and context will be provided by the preloader.
     * @param item     The model to load.
     * @param position The position of the model in the adapter
     * @return A non null {@link BitmapRequestBuilder}.
     */
    @SuppressWarnings("rawtypes")
    protected GenericRequestBuilder getRequestBuilder(T item, int position) {
        return this.preloadModelProvider.getPreloadRequestBuilder(item, position);
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
                preloadItem(getItem(i), i);
            }
        } else {
            // Decreasing
            for (int i = end - 1; i >= start; i--) {
                preloadItem(getItem(i), i);
            }
        }

        lastStart = start;
        lastEnd = end;
    }

    @SuppressWarnings("unchecked")
    private void preloadItem(T item, int position) {
        final int[] dimensions = getDimensions(item, position);
        if (dimensions != null) {
            getRequestBuilder(item, position).into(preloadTargetQueue.next(dimensions[0], dimensions[1]));
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
