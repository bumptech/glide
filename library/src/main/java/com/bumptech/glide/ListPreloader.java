package com.bumptech.glide;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.AbsListView;
import com.bumptech.glide.request.target.BaseTarget;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Loads a few images ahead in the direction of scrolling in any {@link AbsListView} so that images are in the memory
 * cache just before the corresponding view in created in the list. Gives the appearance of an infinitely large image
 * cache, depending on scrolling speed, cpu speed, and cache size.
 *
 * <p>
 *  Must be set using {@link AbsListView#setOnScrollListener(android.widget.AbsListView.OnScrollListener)}, or have its
 *  corresponding methods called from another {@link android.widget.AbsListView.OnScrollListener} to function.
 * </p>
 *
 * @param <T> The type of the model being displayed in the list.
 */
public abstract class ListPreloader<T> implements AbsListView.OnScrollListener {
    private final int maxPreload;
    private final PreloadTargetQueue preloadTargetQueue;

    private int lastEnd;
    private int lastStart;
    private int lastFirstVisible;
    private int totalItemCount;

    private boolean isIncreasing = true;

    /**
     * Constructor for the preloader.
     *
     * @param maxPreload The maximum number of items in the list to load ahead (corresponds to adapter positions).
     */
    public ListPreloader(int maxPreload) {
        this.maxPreload = maxPreload;
        preloadTargetQueue = new PreloadTargetQueue(maxPreload + 1);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override
    public void onScroll(AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {
        totalItemCount = totalCount;
        if (firstVisible > lastFirstVisible) {
            preload(firstVisible + visibleCount, true);
        } else if (firstVisible < lastFirstVisible) {
            preload(firstVisible, false);
        }
        lastFirstVisible = firstVisible;
    }

    /**
     * Returns the dimensions of the view in the list where the images will be displayed.
     * <p>
     *     Note - The dimensions returned here must precisely match those of the view in the list.
     * </p>
     * @param item A model
     * @return The dimensions of the view where the item will be displayed
     */
    protected abstract int[] getDimensions(T item);

    /**
     * Returns a list of all models that need to be loaded for the list to display adapter items start - end. A list of
     * any size can be returned so there can be multiple models per adapter position.
     *
     * @param start The smallest adapter position. Will be >= 0 && < adapter.getCount() && <= end
     * @param end The largest adapter position. Will be >= 0 && < adapter.getCount && >= start
     * @return A non null list of all models for adapter positions between start and end.
     */
    protected abstract List<T> getItems(int start, int end);

    /**
     * Returns a glide request for a given item. Must exactly match the request used to load the image in the list. The
     * target and context will be provided by the preloader.
     *
     * @param item The model to load.
     * @return A non null {@link BitmapRequestBuilder}.
     */
    protected abstract GenericRequestBuilder getRequestBuilder(T item);

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

    private void preloadItem(List<T> items, int position) {
        final T item = items.get(position);
        final int[] dimensions = getDimensions(item);
        if (dimensions != null) {
            getRequestBuilder(item).into(preloadTargetQueue.next(dimensions[0], dimensions[1]));
        }
    }

    private void cancelAll() {
        for (int i = 0; i < maxPreload; i++) {
            Glide.clear(preloadTargetQueue.next(0, 0));
        }
    }

    private static class PreloadTargetQueue {
        private final Queue<PreloadTarget> queue;

        @TargetApi(9)
        private PreloadTargetQueue(int size) {
            if (Build.VERSION.SDK_INT >= 9) {
                queue = new ArrayDeque<PreloadTarget>(size);
            } else {
                queue = new LinkedList<PreloadTarget>();
            }

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

    private static class PreloadTarget extends BaseTarget {
        private int photoHeight;
        private int photoWidth;

        @Override
        public void onResourceReady(Object resource) {
        }

        @Override
        public void getSize(SizeReadyCallback cb) {
            cb.onSizeReady(photoWidth, photoHeight);
        }

    }
}
