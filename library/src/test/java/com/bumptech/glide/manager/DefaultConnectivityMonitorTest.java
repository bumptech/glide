package com.bumptech.glide.manager;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetworkInfo;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class DefaultConnectivityMonitorTest {
  private ConnectivityMonitor.ConnectivityListener listener;
  private DefaultConnectivityMonitor monitor;

  @Before
  public void setUp() {
    listener = mock(ConnectivityMonitor.ConnectivityListener.class);
    monitor = new DefaultConnectivityMonitor(RuntimeEnvironment.application, listener);
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
    ConnectivityHarness harness = new ConnectivityHarness();
    harness.connect();

    monitor.onStart();
    harness.broadcast();

    verify(listener, never()).onConnectivityChanged(anyBoolean());
  }

  @Test
  public void testNotifiesListenerIfConnectedAndBecomesDisconnected() {
    ConnectivityHarness harness = new ConnectivityHarness();
    harness.connect();

    monitor.onStart();
    harness.disconnect();
    harness.broadcast();

    verify(listener).onConnectivityChanged(eq(false));
  }

  @Test
  public void testNotifiesListenerIfDisconnectedAndBecomesConnected() {
    ConnectivityHarness harness = new ConnectivityHarness();
    harness.disconnect();

    monitor.onStart();
    harness.connect();
    harness.broadcast();

    verify(listener).onConnectivityChanged(eq(true));
  }

  @Test
  public void testDoesNotNotifyListenerWhenNotRegistered() {
    ConnectivityHarness harness = new ConnectivityHarness();
    harness.disconnect();

    monitor.onStart();
    monitor.onStop();
    harness.connect();
    harness.broadcast();

    verify(listener, never()).onConnectivityChanged(anyBoolean());
  }

  private List<BroadcastReceiver> getConnectivityReceivers() {
    Intent connectivity = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
    return ShadowApplication.getInstance().getReceiversForIntent(connectivity);
  }

  private static class ConnectivityHarness {
    private final ShadowConnectivityManager shadowConnectivityManager;

    public ConnectivityHarness() {
      ConnectivityManager connectivityManager = (ConnectivityManager) RuntimeEnvironment.application
          .getSystemService(Context.CONNECTIVITY_SERVICE);
      shadowConnectivityManager = Shadows.shadowOf(connectivityManager);
    }

    public void disconnect() {
      shadowConnectivityManager.setActiveNetworkInfo(null);
    }

    public void connect() {
      NetworkInfo networkInfo =
          ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED, 0, 0, true, true);
      shadowConnectivityManager.setActiveNetworkInfo(networkInfo);
    }

    public void broadcast() {
      Intent connected = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
      ShadowApplication.getInstance().sendBroadcast(connected);
    }
  }
}
