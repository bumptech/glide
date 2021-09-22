package com.bumptech.glide.manager;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.util.Synthetic;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A view-less {@link android.app.Fragment} used to safely store an {@link
 * com.bumptech.glide.RequestManager} that can be used to start, stop and manage Glide requests
 * started for targets the fragment or activity this fragment is a child of.
 *
 * @see com.bumptech.glide.manager.SupportRequestManagerFragment
 * @see com.bumptech.glide.manager.RequestManagerRetriever
 * @see com.bumptech.glide.RequestManager
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public class RequestManagerFragment extends Fragment {
  private static final String TAG = "RMFragment";
  private final ActivityFragmentLifecycle lifecycle;
  private final RequestManagerTreeNode requestManagerTreeNode =
      new FragmentRequestManagerTreeNode();

  @SuppressWarnings("deprecation")
  private final Set<RequestManagerFragment> childRequestManagerFragments = new HashSet<>();

  @Nullable private RequestManager requestManager;

  @SuppressWarnings("deprecation")
  @Nullable
  private RequestManagerFragment rootRequestManagerFragment;

  @Nullable private Fragment parentFragmentHint;

  public RequestManagerFragment() {
    this(new ActivityFragmentLifecycle());
  }

  @VisibleForTesting
  @SuppressLint("ValidFragment")
  RequestManagerFragment(@NonNull ActivityFragmentLifecycle lifecycle) {
    this.lifecycle = lifecycle;
  }

  /**
   * Sets the current {@link com.bumptech.glide.RequestManager}.
   *
   * @param requestManager The request manager to use.
   */
  public void setRequestManager(@Nullable RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  @NonNull
  ActivityFragmentLifecycle getGlideLifecycle() {
    return lifecycle;
  }

  /** Returns the current {@link com.bumptech.glide.RequestManager} or null if none exists. */
  @Nullable
  public RequestManager getRequestManager() {
    return requestManager;
  }

  /** Returns the {@link RequestManagerTreeNode} for this fragment. */
  @NonNull
  public RequestManagerTreeNode getRequestManagerTreeNode() {
    return requestManagerTreeNode;
  }

  @SuppressWarnings("deprecation")
  private void addChildRequestManagerFragment(RequestManagerFragment child) {
    childRequestManagerFragments.add(child);
  }

  @SuppressWarnings("deprecation")
  private void removeChildRequestManagerFragment(RequestManagerFragment child) {
    childRequestManagerFragments.remove(child);
  }

  /**
   * Returns the set of fragments that this RequestManagerFragment's parent is a parent to. (i.e.
   * our parent is the fragment that we are annotating).
   */
  @SuppressWarnings("deprecation")
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  @Synthetic
  @NonNull
  Set<RequestManagerFragment> getDescendantRequestManagerFragments() {
    if (equals(rootRequestManagerFragment)) {
      return Collections.unmodifiableSet(childRequestManagerFragments);
    } else if (rootRequestManagerFragment == null
        || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      // Pre JB MR1 doesn't allow us to get the parent fragment so we can't introspect hierarchy,
      // so just return an empty set.
      return Collections.emptySet();
    } else {
      Set<RequestManagerFragment> descendants = new HashSet<>();
      for (RequestManagerFragment fragment :
          rootRequestManagerFragment.getDescendantRequestManagerFragments()) {
        if (isDescendant(fragment.getParentFragment())) {
          descendants.add(fragment);
        }
      }
      return Collections.unmodifiableSet(descendants);
    }
  }

  /**
   * Sets a hint for which fragment is our parent which allows the fragment to return correct
   * information about its parents before pending fragment transactions have been executed.
   */
  void setParentFragmentHint(@Nullable Fragment parentFragmentHint) {
    this.parentFragmentHint = parentFragmentHint;
    if (parentFragmentHint != null && parentFragmentHint.getActivity() != null) {
      registerFragmentWithRoot(parentFragmentHint.getActivity());
    }
  }

  @Nullable
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private Fragment getParentFragmentUsingHint() {
    final Fragment fragment;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      fragment = getParentFragment();
    } else {
      fragment = null;
    }
    return fragment != null ? fragment : parentFragmentHint;
  }

  /** Returns true if the fragment is a descendant of our parent. */
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private boolean isDescendant(@NonNull Fragment fragment) {
    Fragment root = getParentFragment();
    Fragment parentFragment;
    while ((parentFragment = fragment.getParentFragment()) != null) {
      if (parentFragment.equals(root)) {
        return true;
      }
      fragment = fragment.getParentFragment();
    }
    return false;
  }

  @SuppressWarnings("deprecation")
  private void registerFragmentWithRoot(@NonNull Activity activity) {
    unregisterFragmentWithRoot();
    rootRequestManagerFragment =
        Glide.get(activity).getRequestManagerRetriever().getRequestManagerFragment(activity);
    if (!equals(rootRequestManagerFragment)) {
      rootRequestManagerFragment.addChildRequestManagerFragment(this);
    }
  }

  private void unregisterFragmentWithRoot() {
    if (rootRequestManagerFragment != null) {
      rootRequestManagerFragment.removeChildRequestManagerFragment(this);
      rootRequestManagerFragment = null;
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      registerFragmentWithRoot(activity);
    } catch (IllegalStateException e) {
      // OnAttach can be called after the activity is destroyed, see #497.
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Unable to register fragment with root", e);
      }
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    unregisterFragmentWithRoot();
  }

  @Override
  public void onStart() {
    super.onStart();
    lifecycle.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();
    lifecycle.onStop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    lifecycle.onDestroy();
    unregisterFragmentWithRoot();
  }

  @Override
  public String toString() {
    return super.toString() + "{parent=" + getParentFragmentUsingHint() + "}";
  }

  private class FragmentRequestManagerTreeNode implements RequestManagerTreeNode {

    @Synthetic
    FragmentRequestManagerTreeNode() {}

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public Set<RequestManager> getDescendants() {
      Set<RequestManagerFragment> descendantFragments = getDescendantRequestManagerFragments();
      Set<RequestManager> descendants = new HashSet<>(descendantFragments.size());
      for (RequestManagerFragment fragment : descendantFragments) {
        if (fragment.getRequestManager() != null) {
          descendants.add(fragment.getRequestManager());
        }
      }
      return descendants;
    }

    @SuppressWarnings("deprecation")
    @Override
    public String toString() {
      return super.toString() + "{fragment=" + RequestManagerFragment.this + "}";
    }
  }
}
