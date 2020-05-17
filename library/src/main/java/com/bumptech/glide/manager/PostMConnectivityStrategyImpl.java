package com.bumptech.glide.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.manager.ConnectivityMonitor.ConnectivityListener;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;

/**
 * A strategy that use {@link ConnectivityManager.NetworkCallback} and {@link
 * ConnectivityManager#getNetworkCapabilities} to perceive connectivity changes.
 */
@RequiresApi(api = VERSION_CODES.M)
public class PostMConnectivityStrategyImpl implements ConnectivityStrategy {
  private static final String TAG = "ConnectivityStrategy";
  private final Context context;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final ConnectivityListener listener;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  boolean isConnected;

  private boolean isRegistered;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final ConnectivityManager.NetworkCallback callback;

  PostMConnectivityStrategyImpl(@NonNull Context context, @NonNull ConnectivityListener listener) {
    this.context = context.getApplicationContext();
    this.listener = listener;
    callback =
        new ConnectivityManager.NetworkCallback() {
          @Override
          public void onAvailable(@NonNull Network network) {
            onConnectivityChanged(true);
          }

          @Override
          public void onLost(@NonNull Network network) {
            onConnectivityChanged(false);
          }

          @Override
          public void onCapabilitiesChanged(
              @NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            boolean isConnected = checkNetworkConnected(networkCapabilities);
            onConnectivityChanged(isConnected);
          }
        };
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  void onConnectivityChanged(boolean isConnected) {
    boolean wasConnected = this.isConnected;
    this.isConnected = isConnected;
    if (wasConnected != isConnected) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "connectivity changed, isConnected: " + isConnected);
      }

      listener.onConnectivityChanged(isConnected);
    }
  }

  @Override
  @SuppressLint("MissingPermission")
  public void register() {
    if (isRegistered) {
      return;
    }
    ConnectivityManager connectivityManager =
        Preconditions.checkNotNull(
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
    // Initialize isConnected.
    isConnected = isConnected(connectivityManager);

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        connectivityManager.registerDefaultNetworkCallback(callback);
      } else {
        NetworkRequest request = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(request, callback);
      }
      isRegistered = true;
    } catch (RuntimeException e) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Failed to register", e);
      }
    }
  }

  @Override
  public void unregister() {
    if (!isRegistered) {
      return;
    }
    ConnectivityManager connectivityManager =
        Preconditions.checkNotNull(
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
    try {
      connectivityManager.unregisterNetworkCallback(callback);
    } catch (RuntimeException e) {
      // callback was registered more than once
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "already unregister", e);
      }
    }
    isRegistered = false;
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  // Permissions are checked in the factory instead.
  @SuppressLint("MissingPermission")
  boolean isConnected(@NonNull ConnectivityManager connectivityManager) {
    NetworkCapabilities networkCapabilities;
    try {
      networkCapabilities =
          connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
    } catch (RuntimeException e) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Failed to determine connectivity status when connectivity changed", e);
      }
      // Default to true;
      return true;
    }
    return checkNetworkConnected(networkCapabilities);
  }

  public static boolean checkNetworkConnected(@Nullable NetworkCapabilities networkCapabilities) {
    return networkCapabilities != null
        && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
  }
}
