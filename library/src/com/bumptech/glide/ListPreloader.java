package com.bumptech.glide;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.widget.AbsListView;
import com.bumptech.glide.presenter.target.SimpleTarget;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class ListPreloader<T> implements AbsListView.OnScrollListener {
    private final int maxPreload;
    private final Context context;
    private final PreloadTargetQueue preloadTargetQueue;

    private int lastEnd;
    private int lastStart;
    private int lastFirstVisible;
    private int totalItemCount;

    private boolean isIncreasing = true;

    public ListPreloader(Context context, int maxPreload) {
        this.context = context;
        this.maxPreload = maxPreload;
        preloadTargetQueue = new PreloadTargetQueue(maxPreload);
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

    protected abstract int[] getDimens(T item);

    protected abstract List<T> getItems(int start, int end);

    protected abstract Glide.Request<T> getRequest(T item);

    public void preload(int start, boolean increasing) {
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

        // Increasing
        if (from < to) {
            final int numItems = items.size();
            for (int i = 0; i < numItems; i++) {
                preload(items, i);
            }
        } else {
            for (int i = items.size() - 1; i >= 0; i--) {
                preload(items, i);
            }
        }

        lastStart = start;
        lastEnd = end;
    }

    private void preload(List<T> items, int position) {
        final T item = items.get(position);
        int[] dimens = getDimens(item);
        getRequest(item).into(preloadTargetQueue.next(dimens[0], dimens[1])).with(context);
    }

    private void cancelAll() {
        for (int i = 0; i < maxPreload; i++) {
            Glide.cancel(preloadTargetQueue.next(0, 0));
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

    private static class PreloadTarget extends SimpleTarget {
        private int photoHeight;
        private int photoWidth;

        @Override
        protected int[] getSize() {
            return new int[] { photoWidth, photoHeight };
        }

        @Override
        public void onImageReady(Bitmap bitmap) { }
    }
}
