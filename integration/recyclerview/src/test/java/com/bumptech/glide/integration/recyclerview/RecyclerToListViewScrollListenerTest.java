package com.bumptech.glide.integration.recyclerview;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class RecyclerToListViewScrollListenerTest {
  @Test
  public void onScrolled_withCustomPositionProvider_forwardsScrollValues() {
    RecyclerView recyclerView = newRecyclerViewWithAdapter(20);
    RecordingOnScrollListener scrollListener = new RecordingOnScrollListener();
    RecyclerToListViewScrollListener listener =
        new RecyclerToListViewScrollListener(
            scrollListener, new FixedPositionProvider(7, 3));

    listener.onScrolled(recyclerView, /* dx= */ 0, /* dy= */ 5);

    assertThat(scrollListener.callCount).isEqualTo(1);
    assertThat(scrollListener.firstVisible).isEqualTo(7);
    assertThat(scrollListener.visibleCount).isEqualTo(3);
    assertThat(scrollListener.totalCount).isEqualTo(20);
  }

  @Test
  public void onScrolled_withLinearLayoutManager_countsVisibleItems() {
    Context context = ApplicationProvider.getApplicationContext();
    RecyclerView recyclerView = newRecyclerViewWithAdapter(20);
    recyclerView.setLayoutManager(new TestLinearLayoutManager(context, 2, 5));
    RecordingOnScrollListener scrollListener = new RecordingOnScrollListener();
    RecyclerToListViewScrollListener listener =
        new RecyclerToListViewScrollListener(scrollListener);

    listener.onScrolled(recyclerView, /* dx= */ 0, /* dy= */ 5);

    assertThat(scrollListener.firstVisible).isEqualTo(2);
    assertThat(scrollListener.visibleCount).isEqualTo(4);
    assertThat(scrollListener.totalCount).isEqualTo(20);
  }

  private static RecyclerView newRecyclerViewWithAdapter(int itemCount) {
    Context context = ApplicationProvider.getApplicationContext();
    RecyclerView recyclerView = new RecyclerView(context);
    recyclerView.setAdapter(new TestAdapter(itemCount));
    return recyclerView;
  }

  private static final class RecordingOnScrollListener implements AbsListView.OnScrollListener {
    int callCount;
    int firstVisible;
    int visibleCount;
    int totalCount;

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(
        AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
      callCount++;
      firstVisible = firstVisibleItem;
      visibleCount = visibleItemCount;
      totalCount = totalItemCount;
    }
  }

  private static final class FixedPositionProvider implements RecyclerViewPositionProvider {
    private final int firstVisible;
    private final int visibleCount;

    FixedPositionProvider(int firstVisible, int visibleCount) {
      this.firstVisible = firstVisible;
      this.visibleCount = visibleCount;
    }

    @Override
    public int getFirstVisiblePosition(@NonNull RecyclerView recyclerView) {
      return firstVisible;
    }

    @Override
    public int getVisibleItemCount(@NonNull RecyclerView recyclerView) {
      return visibleCount;
    }
  }

  private static final class TestLinearLayoutManager extends LinearLayoutManager {
    private final int firstVisible;
    private final int lastVisible;

    TestLinearLayoutManager(Context context, int firstVisible, int lastVisible) {
      super(context);
      this.firstVisible = firstVisible;
      this.lastVisible = lastVisible;
    }

    @Override
    public int findFirstVisibleItemPosition() {
      return firstVisible;
    }

    @Override
    public int findLastVisibleItemPosition() {
      return lastVisible;
    }
  }

  private static final class TestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int itemCount;

    TestAdapter(int itemCount) {
      this.itemCount = itemCount;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      return new RecyclerView.ViewHolder(new View(parent.getContext())) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {}

    @Override
    public int getItemCount() {
      return itemCount;
    }
  }
}
