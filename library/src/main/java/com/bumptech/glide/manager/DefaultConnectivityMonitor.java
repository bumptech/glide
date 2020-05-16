package com.bumptech.glide.manager;

import androidx.annotation.NonNull;
import com.bumptech.glide.util.Synthetic;

/** Uses {@link android.net.ConnectivityManager} to identify connectivity changes. */
public final class DefaultConnectivityMonitor implements ConnectivityMonitor {

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final ConnectivityStrategy strategy;

  DefaultConnectivityMonitor(@NonNull ConnectivityStrategy strategy) {
    this.strategy = strategy;
  }

  @Override
  public void onStart() {
    strategy.register();
  }

  @Override
  public void onStop() {
    strategy.unregister();
  }

  @Override
  public void onDestroy() {
    // Do nothing.
  }
}
