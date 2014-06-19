package com.bumptech.glide.manager;

import android.content.Context;
import com.bumptech.glide.request.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LifecycleRequestManagerTest {
    private LifecycleRequestManager manager;
    private ConnectivityMonitor connectivityMonitor;

    @Before
    public void setUp() {
        connectivityMonitor = mock(ConnectivityMonitor.class);
        ConnectivityMonitorFactory factory = mock(ConnectivityMonitorFactory.class);
        when(factory.build(any(Context.class), any(ConnectivityMonitor.ConnectivityListener.class)))
                .thenReturn(connectivityMonitor);
        manager = new LifecycleRequestManager(Robolectric.application, factory);
    }

    @Test
    public void testCancelsRequestsOnStop() {
        manager.onStart();
        Request request = mock(Request.class);
        manager.addRequest(request);
        manager.onStop();

        verify(request).clear();
    }

    @Test
    public void testCanAddAndRemoveRequest() {
        manager.onStart();
        Request request = mock(Request.class);
        manager.addRequest(request);
        manager.removeRequest(request);

        manager.onStop();

        verify(request, never()).clear();
    }

    @Test
    public void testCanAddMultipleRequests() {
        manager.onStart();
        Request first = mock(Request.class);
        Request second = mock(Request.class);
        manager.addRequest(first);
        manager.addRequest(second);

        manager.onStop();

        verify(first).clear();
        verify(second).clear();
    }

    @Test
    public void testDoesNotClearCompleteRequestsOnStop() {
        manager.onStart();
        Request request = mock(Request.class);
        when(request.isComplete()).thenReturn(true);
        manager.addRequest(request);

        manager.onStop();

        verify(request, never()).clear();
    }

    @Test
    public void testDoesNotClearFailedRequestsOnStop() {
        manager.onStart();
        Request request = mock(Request.class);
        when(request.isFailed()).thenReturn(true);
        manager.addRequest(request);

        manager.onStop();

        verify(request, never()).clear();
    }

    @Test
    public void testRestartsStoppedRequestOnStart() {
        Request request = mock(Request.class);
        manager.addRequest(request);

        manager.onStart();

        verify(request).run();
    }

    @Test
    public void testDoesNotRestartCompletedRequestsOnStart() {
        Request request = mock(Request.class);
        when(request.isComplete()).thenReturn(true);
        manager.addRequest(request);

        manager.onStart();

        verify(request, never()).run();
    }

    @Test
    public void testDoesRestartFailedRequestsOnStart() {
        Request request = mock(Request.class);
        when(request.isFailed()).thenReturn(true);
        manager.addRequest(request);

        manager.onStart();

        verify(request).run();
    }

    @Test
    public void testDoesNotStartStartedRequestsOnStart() {
        Request request = mock(Request.class);
        when(request.isRunning()).thenReturn(true);
        manager.addRequest(request);

        manager.onStart();

        verify(request, never()).run();
    }

    @Test
    public void testClearsAllRequestsOnDestroy() {
        Request first = mock(Request.class);
        Request second = mock(Request.class);
        when(second.isComplete()).thenReturn(true);
        Request third = mock(Request.class);
        when(third.isFailed()).thenReturn(true);
        manager.addRequest(first);
        manager.addRequest(second);
        manager.addRequest(third);

        manager.onDestroy();

        verify(first).clear();
        verify(second).clear();
        verify(third).clear();
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
    public void testRestartsFailedRequestOnConnected() {
        Request request = mock(Request.class);
        when(request.isFailed()).thenReturn(true);
        manager.addRequest(request);

        manager.onConnectivityChanged(true);

        verify(request).run();
    }

    @Test
    public void testCancelsAndRestartsNotYetFinishedRequestsWhenBecomesConnected() {
        Request request = mock(Request.class);
        when(request.isComplete()).thenReturn(false);
        manager.addRequest(request);

        manager.onConnectivityChanged(true);

        verify(request).clear();
        verify(request).run();
    }
}
