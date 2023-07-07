package com.bumptech.glide.manager;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.manager.RequestManagerRetriever.RequestManagerFactory;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class LifecycleRequestManagerRetriever {
  @Synthetic final Map<Lifecycle, RequestManager> lifecycleToRequestManager = new HashMap<>();
  @NonNull private final RequestManagerFactory factory;

  LifecycleRequestManagerRetriever(@NonNull RequestManagerFactory factory) {
    this.factory = factory;
  }

  RequestManager getOnly(Lifecycle lifecycle) {
    Util.assertMainThread();
    return lifecycleToRequestManager.get(lifecycle);
  }

  RequestManager getOrCreate(
      Context context,
      Glide glide,
      final Lifecycle lifecycle,
      FragmentManager childFragmentManager,
      boolean isParentVisible) {
    Util.assertMainThread();
    RequestManager result = getOnly(lifecycle);
    if (result == null) {
      LifecycleLifecycle glideLifecycle = new LifecycleLifecycle(lifecycle);
      result =
          factory.build(
              glide,
              glideLifecycle,
              new SupportRequestManagerTreeNode(childFragmentManager),
              context);
      lifecycleToRequestManager.put(lifecycle, result);
      glideLifecycle.addListener(
          new LifecycleListener() {
            @Override
            public void onStart() {}

            @Override
            public void onStop() {}

            @Override
            public void onDestroy() {
              lifecycleToRequestManager.remove(lifecycle);
            }
          });
      // This is a bit of hack, we're going to start the RequestManager, but not the
      // corresponding Lifecycle. It's safe to start the RequestManager, but starting the
      // Lifecycle might trigger memory leaks. See b/154405040
      if (isParentVisible) {
        result.onStart();
      }
    }
    return result;
  }

  private final class SupportRequestManagerTreeNode implements RequestManagerTreeNode {
    private final FragmentManager childFragmentManager;

    SupportRequestManagerTreeNode(FragmentManager childFragmentManager) {
      this.childFragmentManager = childFragmentManager;
    }

    @NonNull
    @Override
    public Set<RequestManager> getDescendants() {
      Set<RequestManager> result = new HashSet<>();
      getChildFragmentsRecursive(childFragmentManager, result);
      return result;
    }

    private void getChildFragmentsRecursive(
        FragmentManager fragmentManager, Set<RequestManager> requestManagers) {
      List<Fragment> children = fragmentManager.getFragments();
      for (int i = 0, size = children.size(); i < size; i++) {
        Fragment child = children.get(i);
        getChildFragmentsRecursive(child.getChildFragmentManager(), requestManagers);
        RequestManager fromChild = getOnly(child.getLifecycle());
        if (fromChild != null) {
          requestManagers.add(fromChild);
        }
      }
    }
  }
}
