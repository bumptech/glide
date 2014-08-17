package com.bumptech.glide.manager;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetworkInfo;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class DefaultConnectivityMonitorTest {
    private ConnectivityMonitor.ConnectivityListener listener;
    private DefaultConnectivityMonitor monitor;

    @Before
    public void setUp() {
        listener = mock(ConnectivityMonitor.ConnectivityListener.class);
        monitor = new DefaultConnectivityMonitor(Robolectric.application, listener);
    }

    @Test
    public void testRegistersReceiverOnStart() {
        monitor.onStart();

        assertEquals(1, getConnectivityReceiverCount());
    }

    @Test
    public void testDoesNotRegisterTwiceOnStart() {
        monitor.onStart();
        monitor.onStart();

        assertEquals(1, getConnectivityReceiverCount());
    }

    @Test
    public void testUnregistersReceiverOnStop() {
        monitor.onStart();
        monitor.onStop();

        assertEquals(0, getConnectivityReceiverCount());
    }

    @Test
    public void testHandlesUnregisteringTwiceInARow() {
        monitor.onStop();
        monitor.onStop();

        assertEquals(0, getConnectivityReceiverCount());
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

    private int getConnectivityReceiverCount() {
        Intent connectivity = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
        return Robolectric.getShadowApplication().getReceiversForIntent(connectivity).size();
    }

    private static class ConnectivityHarness {
        private final ShadowConnectivityManager shadowConnectivityManager;

        public ConnectivityHarness() {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);
            shadowConnectivityManager = Robolectric.shadowOf(connectivityManager);
        }

        public void disconnect() {
            shadowConnectivityManager.setActiveNetworkInfo(null);
        }

        public void connect() {
            NetworkInfo networkInfo = ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED, 0, 0, true,
                    true);
            shadowConnectivityManager.setActiveNetworkInfo(networkInfo);
        }

        public void broadcast() {
            Intent connected = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
            Robolectric.shadowOf(Robolectric.application).sendBroadcast(connected);
        }
    }
}