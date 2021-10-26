package com.bumptech.glide.manager;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.manager.DefaultConnectivityMonitorTest.PermissionConnectivityManager;
import org.junit.After;
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
import org.robolectric.shadows.ShadowNetwork;
import org.robolectric.shadows.ShadowNetworkInfo;

@LooperMode(LEGACY)
@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = {24},
    shadows = PermissionConnectivityManager.class)
public class DefaultConnectivityMonitorTest {
  @Mock private ConnectivityMonitor.ConnectivityListener listener;
  private DefaultConnectivityMonitor monitor;
  private ConnectivityHarness harness;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    monitor = new DefaultConnectivityMonitor(ApplicationProvider.getApplicationContext(), listener);
    harness =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            ? new ConnectivityHarnessPost24()
            : new ConnectivityHarnessPre24();
  }

  @After
  public void tearDown() {
    SingletonConnectivityReceiver.reset();
  }

  @Test
  public void testRegistersReceiverOnStart() {
    monitor.onStart();

    assertThat(harness.getRegisteredReceivers()).isEqualTo(1);
  }

  @Test
  public void testDoesNotRegisterTwiceOnStart() {
    monitor.onStart();
    monitor.onStart();

    assertThat(harness.getRegisteredReceivers()).isEqualTo(1);
  }

  @Test
  public void testUnregistersReceiverOnStop() {
    monitor.onStart();
    monitor.onStop();

    assertThat(harness.getRegisteredReceivers()).isEqualTo(0);
  }

  @Test
  public void testHandlesUnregisteringTwiceInARow() {
    monitor.onStop();
    monitor.onStop();

    assertThat(harness.getRegisteredReceivers()).isEqualTo(0);
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

    verify(listener).onConnectivityChanged(true);
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
    harness.setNetworkPermissionGranted(false);

    monitor.onStart();
  }

  @Test
  public void onReceive_withMissingPermission_doesNotThrow() {
    monitor.onStart();
    harness.setNetworkPermissionGranted(false);
    harness.broadcast();
  }

  @Test
  public void onReceive_withMissingPermission_previouslyDisconnected_notifiesListenersConnected() {
    harness.disconnect();
    monitor.onStart();
    harness.setNetworkPermissionGranted(false);
    harness.broadcast();

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      verify(listener).onConnectivityChanged(true);
    } else {
      verify(listener, never()).onConnectivityChanged(anyBoolean());
    }
  }

  @Test
  public void onReceive_withMissingPermission_previouslyConnected_doesNotNotifyListeners() {
    harness.connect();
    monitor.onStart();
    harness.setNetworkPermissionGranted(false);
    harness.broadcast();

    verify(listener, never()).onConnectivityChanged(anyBoolean());
  }

  private interface ConnectivityHarness {
    void connect();

    void disconnect();

    void broadcast();

    void setNetworkPermissionGranted(boolean isGranted);

    int getRegisteredReceivers();
  }

  private static final class ConnectivityHarnessPost24 implements ConnectivityHarness {

    private final PermissionConnectivityManager shadowConnectivityManager;

    ConnectivityHarnessPost24() {
      ConnectivityManager connectivityManager =
          (ConnectivityManager)
              ApplicationProvider.getApplicationContext()
                  .getSystemService(Context.CONNECTIVITY_SERVICE);
      shadowConnectivityManager = Shadow.extract(connectivityManager);
    }

    @Override
    public void connect() {
      shadowConnectivityManager.isConnected = true;
    }

    @Override
    public void disconnect() {
      shadowConnectivityManager.isConnected = false;
    }

    @Override
    public void broadcast() {
      for (NetworkCallback callback : shadowConnectivityManager.getNetworkCallbacks()) {
        if (shadowConnectivityManager.isConnected) {
          callback.onAvailable(null);
        } else {
          callback.onLost(null);
        }
      }
    }

    @Override
    public void setNetworkPermissionGranted(boolean isGranted) {
      shadowConnectivityManager.isNetworkPermissionGranted = isGranted;
    }

    @Override
    public int getRegisteredReceivers() {
      return shadowConnectivityManager.getNetworkCallbacks().size();
    }
  }

  private static final class ConnectivityHarnessPre24 implements ConnectivityHarness {
    private final PermissionConnectivityManager shadowConnectivityManager;

    public ConnectivityHarnessPre24() {
      ConnectivityManager connectivityManager =
          (ConnectivityManager)
              ApplicationProvider.getApplicationContext()
                  .getSystemService(Context.CONNECTIVITY_SERVICE);
      shadowConnectivityManager = Shadow.extract(connectivityManager);
    }

    @Override
    public void disconnect() {
      shadowConnectivityManager.setActiveNetworkInfo(null);
    }

    @Override
    public void connect() {
      NetworkInfo networkInfo =
          ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED, 0, 0, true, true);
      shadowConnectivityManager.setActiveNetworkInfo(networkInfo);
    }

    @Override
    public void broadcast() {
      Intent connected = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
      ApplicationProvider.getApplicationContext().sendBroadcast(connected);
    }

    @Override
    public void setNetworkPermissionGranted(boolean isGranted) {
      shadowConnectivityManager.isNetworkPermissionGranted = isGranted;
    }

    @Override
    public int getRegisteredReceivers() {
      Intent connectivity = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
      return shadowOf((Application) ApplicationProvider.getApplicationContext())
          .getReceiversForIntent(connectivity)
          .size();
    }
  }

  @Implements(ConnectivityManager.class)
  public static final class PermissionConnectivityManager extends ShadowConnectivityManager {
    private boolean isNetworkPermissionGranted = true;
    private boolean isConnected;

    @Implementation
    @Override
    public Network getActiveNetwork() {
      if (isConnected) {
        return ShadowNetwork.newInstance(1);
      } else {
        return null;
      }
    }

    @Implementation
    @Override
    protected void registerDefaultNetworkCallback(NetworkCallback networkCallback) {
      if (!isNetworkPermissionGranted) {
        throw new SecurityException();
      }
      super.registerDefaultNetworkCallback(networkCallback);
      if (isConnected) {
        networkCallback.onAvailable(null);
      } else {
        networkCallback.onLost(null);
      }
    }

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
