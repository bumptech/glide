package com.bumptech.glide.manager;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import com.bumptech.glide.manager.PostMConnectivityMonitorTest.PermissionConnectivityManager;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowConnectivityManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, shadows = PermissionConnectivityManager.class)
public class PostMConnectivityMonitorTest {
  @Mock private ConnectivityMonitor.ConnectivityListener listener;
  private PostMConnectivityMonitor monitor;
  private ConnectivityHarness harness;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    monitor = new PostMConnectivityMonitor(RuntimeEnvironment.application, listener);
    harness = new ConnectivityHarness();
  }

  @Test
  public void testRegistersNetworkCallbackOnRegister() {
    monitor.register();
    assertThat(harness.getNetworkCall()).contains(monitor.callback);
  }

  @Test
  public void testDoesNotRegisterTwiceOnRegister() {
    monitor.register();
    monitor.register();
    assertThat(harness.getNetworkCall()).contains(monitor.callback);
  }

  @Test
  public void testUnregisterNetworkCallbackOnUnregister() {
    monitor.register();
    monitor.unregister();
    assertThat(harness.getNetworkCall()).doesNotContain(monitor.callback);
  }

  @Test
  public void testHandlesUnregisteringTwiceInARow() {
    monitor.unregister();
    monitor.unregister();
    assertThat(harness.getNetworkCall()).doesNotContain(monitor.callback);
  }

  @Test
  public void register_withMissingPermission_doesNotThrow() {
    harness.shadowConnectivityManager.isNetworkPermissionGranted = false;
    monitor.register();
  }

  private static class ConnectivityHarness {
    private final PermissionConnectivityManager shadowConnectivityManager;

    public ConnectivityHarness() {
      ConnectivityManager connectivityManager =
          (ConnectivityManager)
              RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE);
      shadowConnectivityManager = Shadow.extract(connectivityManager);
    }

    Set<NetworkCallback> getNetworkCall() {
      return shadowConnectivityManager.getNetworkCallbacks();
    }
  }

  @Implements(ConnectivityManager.class)
  public static final class PermissionConnectivityManager extends ShadowConnectivityManager {
    private boolean isNetworkPermissionGranted = true;

    @Implementation
    @Override
    protected NetworkCapabilities getNetworkCapabilities(Network network) {
      if (!isNetworkPermissionGranted) {
        throw new SecurityException();
      }
      return super.getNetworkCapabilities(network);
    }
  }
}
