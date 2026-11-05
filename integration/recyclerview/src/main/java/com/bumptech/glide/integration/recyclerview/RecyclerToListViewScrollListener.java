package com.bumptech.glide.integration.recyclerview;

import android.widget.AbsListView;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
  private final RecyclerViewPositionProvider positionProvider;
  private int lastFirstVisible = -1;
  private int lastVisibleCount = -1;
  private int lastItemCount = -1;

  public RecyclerToListViewScrollListener(@NonNull AbsListView.OnScrollListener scrollListener) {
    this(scrollListener, new LinearLayoutManagerPositionProvider());
  }

  public RecyclerToListViewScrollListener(
      @NonNull AbsListView.OnScrollListener scrollListener,
      @NonNull RecyclerViewPositionProvider positionProvider) {
    this.scrollListener = scrollListener;
    this.positionProvider = positionProvider;
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
    int firstVisible = positionProvider.getFirstVisiblePosition(recyclerView);
    int visibleCount = positionProvider.getVisibleItemCount(recyclerView);
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

  private static final class LinearLayoutManagerPositionProvider
      implements RecyclerViewPositionProvider {
    @Override
    public int getFirstVisiblePosition(@NonNull RecyclerView recyclerView) {
      return getLayoutManager(recyclerView).findFirstVisibleItemPosition();
    }

    @Override
    public int getVisibleItemCount(@NonNull RecyclerView recyclerView) {
      LinearLayoutManager layoutManager = getLayoutManager(recyclerView);
      int firstVisible = layoutManager.findFirstVisibleItemPosition();
      int lastVisible = layoutManager.findLastVisibleItemPosition();
      if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
        return 0;
      }
      return Math.abs(lastVisible - firstVisible) + 1;
    }

    private static LinearLayoutManager getLayoutManager(RecyclerView recyclerView) {
      return (LinearLayoutManager) recyclerView.getLayoutManager();
    }
  }
}
