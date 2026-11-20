package com.bumptech.glide.integration.recyclerview;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Provides visible adapter position metadata from a {@link RecyclerView}.
 *
 * <p>Implement this interface to use {@link RecyclerViewPreloader} with custom {@link
 * RecyclerView.LayoutManager} implementations that do not extend {@link
 * androidx.recyclerview.widget.LinearLayoutManager}.
 */
public interface RecyclerViewPositionProvider {
  int getFirstVisiblePosition(@NonNull RecyclerView recyclerView);

  int getVisibleItemCount(@NonNull RecyclerView recyclerView);
}
