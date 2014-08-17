package com.bumptech.glide.manager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class ConnectivityMonitorFactoryTest {
    private ConnectivityMonitorFactory factory;

    @Before
    public void setUp() {
        factory = new ConnectivityMonitorFactory();
    }

    @Test
    public void testReturnsDefaultConnectivityMonitorWhenHasPermission() {
        Robolectric.getShadowApplication().grantPermissions("android.permission.ACCESS_NETWORK_STATE");
        assertTrue(factory.build(Robolectric.application, mock(ConnectivityMonitor.ConnectivityListener.class))
                instanceof DefaultConnectivityMonitor);
    }

    @Test
    public void testReturnsNullConnectivityMonitorWhenDoesNotHavePermission() {
        assertTrue(factory.build(Robolectric.application, mock(ConnectivityMonitor.ConnectivityListener.class))
                instanceof NullConnectivityMonitor);
    }
}