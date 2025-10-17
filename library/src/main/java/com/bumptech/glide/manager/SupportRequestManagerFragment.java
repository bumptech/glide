package com.bumptech.glide.manager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.RequestManager;

/**
 * A view-less {@link androidx.fragment.app.Fragment} used to safely store an {@link
 * com.bumptech.glide.RequestManager} that can be used to start, stop and manage Glide requests
 * started for targets within the fragment or activity this fragment is a child of.
 *
 * @see com.bumptech.glide.manager.RequestManagerRetriever
 * @see com.bumptech.glide.RequestManager
 * @deprecated This class is unused by Glide. All functionality has been removed. The class will be
 *     removed in a future version.
 */
@Deprecated
public class SupportRequestManagerFragment extends Fragment {

  /**
   * @deprecated A no-op method. See the class deprecation method for details.
   */
  @Deprecated
  public void setRequestManager(@Nullable RequestManager requestManager) {}

  /**
   * @deprecated Always returns {@code null}. See the class deprecation method for details.
   */
  @Nullable
  @Deprecated
  public RequestManager getRequestManager() {
    return null;
  }

  /**
   * @deprecated Always returns {@link EmptyRequestManagerTreeNode}. See the class deprecation
   *     method for details.
   */
  @Deprecated
  @NonNull
  public RequestManagerTreeNode getRequestManagerTreeNode() {
    return new EmptyRequestManagerTreeNode();
  }
}
