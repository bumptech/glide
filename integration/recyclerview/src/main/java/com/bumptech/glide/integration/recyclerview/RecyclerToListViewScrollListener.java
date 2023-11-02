package com.bumptech.glide.integration.recyclerview;

import android.widget.AbsListView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Converts {@link androidx.recyclerview.widget.RecyclerView.OnScrollListener} events to {@link
 * AbsListView} scroll events.
 *
 * <p>Requires that the recycler view be using a {@link LinearLayoutManager} subclass.
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
    int itemCount = recyclerView.getAdapter().getItemCount();
    int firstVisible = RecyclerView.NO_POSITION;
    int visibleCount = 0;

    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

    if (layoutManager instanceof LinearLayoutManager) {
      LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;

      firstVisible = linearLayoutManager.findFirstVisibleItemPosition();
      visibleCount = Math.abs(firstVisible - linearLayoutManager.findLastVisibleItemPosition());
    } else if (layoutManager instanceof StaggeredGridLayoutManager) {
      StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;

      int[] firstVisiblePositions = staggeredGridLayoutManager.findFirstVisibleItemPositions(null);

      if (firstVisiblePositions.length > 0) {
        firstVisible = firstVisiblePositions[0];

        int[] lastVisiblePositions = staggeredGridLayoutManager.findLastVisibleItemPositions(null);

        if (lastVisiblePositions.length > 0) {
          int lastVisible = lastVisiblePositions[0];
          visibleCount = Math.abs(firstVisible - lastVisible);
        }
      }
    }

    if (firstVisible != lastFirstVisible
        || visibleCount != lastVisibleCount
        || itemCount != lastItemCount) {
      scrollListener.onScroll(null, firstVisible, visibleCount, itemCount);
      lastFirstVisible = firstVisible;
      lastVisibleCount = visibleCount;
      lastItemCount = itemCount;
    }
  }
}