package com.bumptech.glide.manager;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.manager.DefaultConnectivityMonitorTest.PermissionConnectivityManager;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetworkInfo;

@LooperMode(LEGACY)
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18, shadows = PermissionConnectivityManager.class)
public class DefaultConnectivityMonitorTest {
  @Mock private ConnectivityMonitor.ConnectivityListener listener;
  private DefaultConnectivityMonitor monitor;
  private ConnectivityHarness harness;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    monitor = new DefaultConnectivityMonitor(ApplicationProvider.getApplicationContext(), listener);
    harness = new ConnectivityHarness();
  }

  @Test
  public void testRegistersReceiverOnStart() {
    monitor.onStart();

    assertThat(getConnectivityReceivers()).hasSize(1);
  }

  @Test
  public void testDoesNotRegisterTwiceOnStart() {
    monitor.onStart();
    monitor.onStart();

    assertThat(getConnectivityReceivers()).hasSize(1);
  }

  @Test
  public void testUnregistersReceiverOnStop() {
    monitor.onStart();
    monitor.onStop();

    assertThat(getConnectivityReceivers()).isEmpty();
  }

  @Test
  public void testHandlesUnregisteringTwiceInARow() {
    monitor.onStop();
    monitor.onStop();

    assertThat(getConnectivityReceivers()).isEmpty();
  }

  @Test
  public void testDoesNotNotifyListenerIfConnectedAndBecomesConnected() {
    harness.connect();

    monitor.onStart();
    harness.broadcast();

    verify(listener, never()).onConnectivityChanged(anyBoolean());
  }

  @Test
  public void testNotifiesListenerIfConnectedAndBecomesDisconnected() {
    harness.connect();

    monitor.onStart();
    harness.disconnect();
    harness.broadcast();

    verify(listener).onConnectivityChanged(eq(false));
  }

  @Test
  public void testNotifiesListenerIfDisconnectedAndBecomesConnected() {
    harness.disconnect();

    monitor.onStart();
    harness.connect();
    harness.broadcast();

    verify(listener).onConnectivityChanged(eq(true));
  }

  @Test
  public void testDoesNotNotifyListenerWhenNotRegistered() {
    harness.disconnect();

    monitor.onStart();
    monitor.onStop();
    harness.connect();
    harness.broadcast();

    verify(listener, never()).onConnectivityChanged(anyBoolean());
  }

  @Test
  public void register_withMissingPermission_doesNotThrow() {
    harness.shadowConnectivityManager.isNetworkPermissionGranted = false;

    monitor.onStart();
  }

  @Test
  public void onReceive_withMissingPermission_doesNotThrow() {
    monitor.onStart();
    harness.shadowConnectivityManager.isNetworkPermissionGranted = false;
    harness.broadcast();
  }

  @Test
  public void onReceive_withMissingPermission_previouslyDisconnected_notifiesListenersConnected() {
    harness.disconnect();
    monitor.onStart();
    harness.shadowConnectivityManager.isNetworkPermissionGranted = false;
    harness.broadcast();

    verify(listener).onConnectivityChanged(true);
  }

  @Test
  public void onReceive_withMissingPermission_previouslyConnected_doesNotNotifyListeners() {
    harness.connect();
    monitor.onStart();
    harness.shadowConnectivityManager.isNetworkPermissionGranted = false;
    harness.broadcast();

    verify(listener, never()).onConnectivityChanged(anyBoolean());
  }

  private List<BroadcastReceiver> getConnectivityReceivers() {
    Intent connectivity = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
    return shadowOf((Application) ApplicationProvider.getApplicationContext())
        .getReceiversForIntent(connectivity);
  }

  private static class ConnectivityHarness {
    private final PermissionConnectivityManager shadowConnectivityManager;

    public ConnectivityHarness() {
      ConnectivityManager connectivityManager =
          (ConnectivityManager)
              ApplicationProvider.getApplicationContext()
                  .getSystemService(Context.CONNECTIVITY_SERVICE);
      shadowConnectivityManager = Shadow.extract(connectivityManager);
    }

    void disconnect() {
      shadowConnectivityManager.setActiveNetworkInfo(null);
    }

    void connect() {
      NetworkInfo networkInfo =
          ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED, 0, 0, true, true);
      shadowConnectivityManager.setActiveNetworkInfo(networkInfo);
    }

    void broadcast() {
      Intent connected = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
      ApplicationProvider.getApplicationContext().sendBroadcast(connected);
    }
  }

  @Implements(ConnectivityManager.class)
  public static final class PermissionConnectivityManager extends ShadowConnectivityManager {
    private boolean isNetworkPermissionGranted = true;

    @Implementation
    @Override
    public NetworkInfo getActiveNetworkInfo() {
      if (!isNetworkPermissionGranted) {
        throw new SecurityException();
      }
      return super.getActiveNetworkInfo();
    }
  }
}
