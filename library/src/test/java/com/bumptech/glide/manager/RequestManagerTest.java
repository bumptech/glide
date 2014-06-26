package com.bumptech.glide.manager;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class RequestManagerTest {
    private RequestManager manager;
    private ConnectivityMonitor connectivityMonitor;
    private RequestTracker requestTracker;
    private ConnectivityMonitor.ConnectivityListener connectivityListener;

    @Before
    public void setUp() {
        connectivityMonitor = mock(ConnectivityMonitor.class);
        ConnectivityMonitorFactory factory = mock(ConnectivityMonitorFactory.class);
        when(factory.build(any(Context.class), any(ConnectivityMonitor.ConnectivityListener.class)))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        connectivityListener = (ConnectivityMonitor.ConnectivityListener) invocation.getArguments()[1];
                        return connectivityMonitor;
                    }
                });
        requestTracker = mock(RequestTracker.class);
        manager = new RequestManager(Robolectric.application, requestTracker, factory);
    }

    @Test
    public void testPausesRequestsOnStop() {
        manager.onStart();
        manager.onStop();

        verify(requestTracker).pauseRequests();
    }

    @Test
    public void testResumesRequestsOnStart() {
        manager.onStart();

        verify(requestTracker).resumeRequests();
    }

    @Test
    public void testClearsRequestsOnDestroy() {
        manager.onDestroy();

        verify(requestTracker).clearRequests();
    }

    @Test
    public void testRegistersConnectivityReceiverWhenConstructed() {
        verify(connectivityMonitor).register();
    }

    @Test
    public void testRegistersConnectivityReceiverOnStart() {
        manager.onStart();

        verify(connectivityMonitor, times(2)).register();
    }

    @Test
    public void testUnregistersConnectivityReceiverOnStop() {
        manager.onStop();

        verify(connectivityMonitor).unregister();
    }

    @Test
    public void testRestartsRequestOnConnected() {
        connectivityListener.onConnectivityChanged(true);

        verify(requestTracker).restartRequests();
    }

    @Test
    public void testDoesNotRestartRequestsOnDisconnected() {
        connectivityListener.onConnectivityChanged(false);

        verify(requestTracker, never()).restartRequests();
    }
}
