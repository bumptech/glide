package com.bumptech.glide.manager;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.util.Synthetic;

/**
 * An Android Lifecycle wrapper that uses {@link SingletonConnectivityReceiver} to observer
 * connectivity changes, allowing for registration to be removed when our listener is being
 * destroyed as part of the Android lifecycle.
 */
final class DefaultConnectivityMonitor implements ConnectivityMonitor {
  private final Context context;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final ConnectivityListener listener;

  DefaultConnectivityMonitor(@NonNull Context context, @NonNull ConnectivityListener listener) {
    this.context = context.getApplicationContext();
    this.listener = listener;
  }

  private void register() {
    SingletonConnectivityReceiver.get(context).register(listener);
  }

  private void unregister() {
    SingletonConnectivityReceiver.get(context).unregister(listener);
  }

  @Override
  public void onStart() {
    register();
  }

  @Override
  public void onStop() {
    unregister();
  }

  @Override
  public void onDestroy() {
    // Do nothing.
  }
}
