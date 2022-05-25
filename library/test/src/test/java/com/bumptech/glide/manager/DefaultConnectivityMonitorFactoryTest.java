package com.bumptech.glide.manager;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class DefaultConnectivityMonitorFactoryTest {
  private ConnectivityMonitorFactory factory;

  @Before
  public void setUp() {
    factory = new DefaultConnectivityMonitorFactory();
  }

  @Test
  public void testReturnsDefaultConnectivityMonitorWhenHasPermission() {
    shadowOf((Application) ApplicationProvider.getApplicationContext())
        .grantPermissions("android.permission.ACCESS_NETWORK_STATE");
    ConnectivityMonitor connectivityMonitor =
        factory.build(
            ApplicationProvider.getApplicationContext(),
            mock(ConnectivityMonitor.ConnectivityListener.class));
    assertThat(connectivityMonitor).isInstanceOf(DefaultConnectivityMonitor.class);
  }

  @Test
  public void testReturnsNullConnectivityMonitorWhenDoesNotHavePermission() {
    ConnectivityMonitor connectivityMonitor =
        factory.build(
            ApplicationProvider.getApplicationContext(),
            mock(ConnectivityMonitor.ConnectivityListener.class));
    assertThat(connectivityMonitor).isInstanceOf(NullConnectivityMonitor.class);
  }
}
