package com.bumptech.glide.manager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

final class ArchLifecycleRegistry {

  private static final Map<android.arch.lifecycle.Lifecycle, RequestManager> REGISTRY =
      new WeakHashMap<>();

  private ArchLifecycleRegistry() {
  }

  @Nullable
  static RequestManager get(@NonNull android.arch.lifecycle.Lifecycle lifecycle) {
    return REGISTRY.get(lifecycle);
  }

  static void set(@NonNull android.arch.lifecycle.Lifecycle lifecycle,
                  @NonNull RequestManager requestManager) {
    REGISTRY.put(lifecycle, requestManager);
  }

  @NonNull
  static RequestManager build(@NonNull RequestManagerRetriever.RequestManagerFactory factory,
                              @NonNull Glide glide,
                              @NonNull android.arch.lifecycle.Lifecycle lifecycle) {
    return factory.build(glide, new ArchLifecycle(lifecycle), new RequestManagerTreeNode() {
      @NonNull
      @Override
      public Set<RequestManager> getDescendants() {
        return Collections.emptySet();
      }
    }, glide.getContext());
  }

}
