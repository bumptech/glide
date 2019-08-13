package com.bumptech.glide.integration.recyclerview;

import android.widget.AbsListView;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Converts {@link androidx.recyclerview.widget.RecyclerView.OnScrollListener} events to {@link
 * AbsListView} scroll events.
 *
 * <p>Requires that the the recycler view be using a {@link LinearLayoutManager} subclass.
 */
// Public API.
@SuppressWarnings("WeakerAccess")
public final class RecyclerToListViewScrollListener extends RecyclerView.OnScrollListener {
  public static final int UNKNOWN_SCROLL_STATE = Integer.MIN_VALUE;
  private final AbsListView.OnScrollListener scrollListener;
  private int lastFirstVisible = -1;
  private int lastVisibleCount = -1;
  private int lastItemCount = -1;

  public RecyclerToListViewScrollListener(@NonNull AbsListView.OnScrollListener scrollListener) {
    this.scrollListener = scrollListener;
  }

  @Override
  public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
    int listViewState;
    switch (newState) {
      case RecyclerView.SCROLL_STATE_DRAGGING:
        listViewState = ListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
        break;
      case RecyclerView.SCROLL_STATE_IDLE:
        listViewState = ListView.OnScrollListener.SCROLL_STATE_IDLE;
        break;
      case RecyclerView.SCROLL_STATE_SETTLING:
        listViewState = ListView.OnScrollListener.SCROLL_STATE_FLING;
        break;
      default:
        listViewState = UNKNOWN_SCROLL_STATE;
    }

    scrollListener.onScrollStateChanged(null /*view*/, listViewState);
  }

  @Override
  public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    LayoutManager layoutManager = recyclerView.getLayoutManager();
    int firstVisible;
    int lastVisible;
    if (layoutManager instanceof LinearLayoutManager) {
      LinearLayoutManager llm = (LinearLayoutManager) layoutManager;
      firstVisible = llm.findFirstVisibleItemPosition();
      lastVisible = llm.findLastVisibleItemPosition();
    } else if (layoutManager instanceof StaggeredGridLayoutManager) {
      StaggeredGridLayoutManager sglm = (StaggeredGridLayoutManager) layoutManager;
      firstVisible = StaggeredGridLayoutHelper.findFirstVisibleItemPosition(sglm);
      lastVisible = StaggeredGridLayoutHelper.findLastVisibleItemPosition(sglm);
    } else {
      throw new UnsupportedOperationException(
          "Only LinearLayoutManager and StaggeredGridLayoutManager types are supported!");
    }

    int visibleCount = Math.abs(firstVisible - lastVisible);
    int itemCount = recyclerView.getAdapter().getItemCount();

    if (firstVisible != lastFirstVisible
        || visibleCount != lastVisibleCount
        || itemCount != lastItemCount) {
      scrollListener.onScroll(null, firstVisible, visibleCount, itemCount);
      lastFirstVisible = firstVisible;
      lastVisibleCount = visibleCount;
      lastItemCount = itemCount;
    }
  }

  static class StaggeredGridLayoutHelper {
    private static int[] itemPositionsHolder;

    static int findFirstVisibleItemPosition(
        StaggeredGridLayoutManager staggeredGridLayoutManager) {
      if (itemPositionsHolder == null) {
        itemPositionsHolder = new int[staggeredGridLayoutManager.getSpanCount()];
      }
      return min(staggeredGridLayoutManager.findFirstVisibleItemPositions(itemPositionsHolder));
    }

    static int findLastVisibleItemPosition(
        StaggeredGridLayoutManager staggeredGridLayoutManager) {
      if (itemPositionsHolder == null) {
        itemPositionsHolder = new int[staggeredGridLayoutManager.getSpanCount()];
      }
      return max(staggeredGridLayoutManager.findLastVisibleItemPositions(itemPositionsHolder));
    }

    private static int min(int[] a) {
      int min = Integer.MAX_VALUE;
      for (int i : a) {
        if (i < min) {
          min = i;
        }
      }
      return min;
    }

    private static int max(int[] a) {
      int max = Integer.MIN_VALUE;
      for (int i : a) {
        if (i > max) {
          max = i;
        }
      }
      return max;
    }
  }
}
