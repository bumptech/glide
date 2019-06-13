package com.bumptech.glide.manager;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

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
    ShadowApplication.getInstance().grantPermissions("android.permission.ACCESS_NETWORK_STATE");
    ConnectivityMonitor connectivityMonitor =
        factory.build(
            RuntimeEnvironment.application, mock(ConnectivityMonitor.ConnectivityListener.class));
    assertThat(connectivityMonitor).isInstanceOf(DefaultConnectivityMonitor.class);
  }

  @Test
  public void testReturnsNullConnectivityMonitorWhenDoesNotHavePermission() {
    ConnectivityMonitor connectivityMonitor =
        factory.build(
            RuntimeEnvironment.application, mock(ConnectivityMonitor.ConnectivityListener.class));
    assertThat(connectivityMonitor).isInstanceOf(NullConnectivityMonitor.class);
  }
}
