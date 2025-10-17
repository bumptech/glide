package com.bumptech.glide.manager;

import android.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.RequestManager;
import java.util.Collections;
import java.util.Set;

/**
 * @deprecated This class is unused by Glide and contains only no-op methods. It's retained along
 *     with its public methods to avoid breaking binary compatibility. Lifecycle integration is no
 *     longer supported outside of androidx Activitys and Fragments.
 */
@Deprecated
public class RequestManagerFragment extends Fragment {
  /**
   * @deprecated This method is a no-op. See the class comment for deprecation details.
   */
  @Deprecated
  public void setRequestManager(@Nullable RequestManager requestManager) {}

  /**
   * @deprecated This always returns null. See the class comment for deprecation details.
   */
  @Deprecated
  @Nullable
  public RequestManager getRequestManager() {
    return null;
  }

  /**
   * @deprecated This always returns an empty tree node. See the class comment for deprecation
   *     details.
   */
  @Deprecated
  @NonNull
  public RequestManagerTreeNode getRequestManagerTreeNode() {
    return new RequestManagerTreeNode() {
      @NonNull
      @Override
      public Set<RequestManager> getDescendants() {
        return Collections.emptySet();
      }
    };
  }
}
