package com.bumptech.glide.manager;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;
import com.bumptech.glide.RequestManager;
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
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class RequestManagerFragment extends Fragment {
  private static final String TAG = "RMFragment";
  private final ActivityFragmentLifecycle lifecycle;
  private final RequestManagerTreeNode requestManagerTreeNode =
      new FragmentRequestManagerTreeNode();
  private final HashSet<RequestManagerFragment> childRequestManagerFragments =
      new HashSet<>();

  @Nullable private RequestManager requestManager;
  @Nullable private RequestManagerFragment rootRequestManagerFragment;
  @Nullable private Fragment parentFragmentHint;

  public RequestManagerFragment() {
    this(new ActivityFragmentLifecycle());
  }

  // For testing only.
  @SuppressLint("ValidFragment")
  RequestManagerFragment(ActivityFragmentLifecycle lifecycle) {
    this.lifecycle = lifecycle;
  }

  /**
   * Sets the current {@link com.bumptech.glide.RequestManager}.
   *
   * @param requestManager The request manager to use.
   */
  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  ActivityFragmentLifecycle getLifecycle() {
    return lifecycle;
  }

  /**
   * Returns the current {@link com.bumptech.glide.RequestManager} or null if none exists.
   */
  @Nullable
  public RequestManager getRequestManager() {
    return requestManager;
  }

  /**
   * Returns the {@link RequestManagerTreeNode} for this fragment.
   */
  public RequestManagerTreeNode getRequestManagerTreeNode() {
    return requestManagerTreeNode;
  }

  private void addChildRequestManagerFragment(RequestManagerFragment child) {
    childRequestManagerFragments.add(child);
  }

  private void removeChildRequestManagerFragment(RequestManagerFragment child) {
    childRequestManagerFragments.remove(child);
  }

  /**
   * Returns the set of fragments that this RequestManagerFragment's parent is a parent to. (i.e.
   * our parent is the fragment that we are annotating).
   */
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  public Set<RequestManagerFragment> getDescendantRequestManagerFragments() {
    if (rootRequestManagerFragment == this) {
      return Collections.unmodifiableSet(childRequestManagerFragments);
    } else if (rootRequestManagerFragment == null
        || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      // Pre JB MR1 doesn't allow us to get the parent fragment so we can't introspect hierarchy,
      // so just return an empty set.
      return Collections.emptySet();
    } else {
      HashSet<RequestManagerFragment> descendants = new HashSet<>();
      for (RequestManagerFragment fragment : rootRequestManagerFragment
          .getDescendantRequestManagerFragments()) {
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
  void setParentFragmentHint(Fragment parentFragmentHint) {
    this.parentFragmentHint = parentFragmentHint;
    if (parentFragmentHint != null && parentFragmentHint.getActivity() != null) {
      registerFragmentWithRoot(parentFragmentHint.getActivity());
    }
  }

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

  /**
   * Returns true if the fragment is a descendant of our parent.
   */
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private boolean isDescendant(Fragment fragment) {
    Fragment root = this.getParentFragment();
    while (fragment.getParentFragment() != null) {
      if (fragment.getParentFragment() == root) {
        return true;
      }
      fragment = fragment.getParentFragment();
    }
    return false;
  }

  private void registerFragmentWithRoot(Activity activity) {
    unregisterFragmentWithRoot();
    rootRequestManagerFragment = RequestManagerRetriever.get()
        .getRequestManagerFragment(activity.getFragmentManager(), null);
    if (rootRequestManagerFragment != this) {
      rootRequestManagerFragment.addChildRequestManagerFragment(this);
    }
  }

  private void unregisterFragmentWithRoot() {
    if (rootRequestManagerFragment != null) {
      rootRequestManagerFragment.removeChildRequestManagerFragment(this);
      rootRequestManagerFragment = null;
    }
  }

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
  public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    // If an activity is re-created, onTrimMemory may be called before a manager is ever put.
    // See #329.
    if (requestManager != null) {
      requestManager.onTrimMemory(level);
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    // If an activity is re-created, onLowMemory may be called before a manager is ever put.
    // See #329.
    if (requestManager != null) {
      requestManager.onLowMemory();
    }
  }

  @Override
  public String toString() {
    return super.toString() + "{parent=" + getParentFragmentUsingHint() + "}";
  }

  private class FragmentRequestManagerTreeNode implements RequestManagerTreeNode {
    @Override
    public Set<RequestManager> getDescendants() {
      Set<RequestManagerFragment> descendantFragments = getDescendantRequestManagerFragments();
      HashSet<RequestManager> descendants = new HashSet<>(descendantFragments.size());
      for (RequestManagerFragment fragment : descendantFragments) {
        if (fragment.getRequestManager() != null) {
          descendants.add(fragment.getRequestManager());
        }
      }
      return descendants;
    }

    @Override
    public String toString() {
      return super.toString() + "{fragment=" + RequestManagerFragment.this + "}";
    }
  }
}
