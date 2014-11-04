package com.bumptech.glide.manager;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ConnectivityMonitorFactoryTest {
    private ConnectivityMonitorFactory factory;

    @Before
    public void setUp() {
        factory = new ConnectivityMonitorFactory();
    }

    @Test
    public void testReturnsDefaultConnectivityMonitorWhenHasPermission() {
        Robolectric.getShadowApplication().grantPermissions("android.permission.ACCESS_NETWORK_STATE");
        ConnectivityMonitor connectivityMonitor =
                factory.build(Robolectric.application, mock(ConnectivityMonitor.ConnectivityListener.class));
        assertThat(connectivityMonitor).isInstanceOf(DefaultConnectivityMonitor.class);
    }

    @Test
    public void testReturnsNullConnectivityMonitorWhenDoesNotHavePermission() {
        ConnectivityMonitor connectivityMonitor =
                factory.build(Robolectric.application, mock(ConnectivityMonitor.ConnectivityListener.class));
        assertThat(connectivityMonitor).isInstanceOf(NullConnectivityMonitor.class);
    }
}
