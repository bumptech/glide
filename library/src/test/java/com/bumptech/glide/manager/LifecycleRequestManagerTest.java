package com.bumptech.glide.manager;

import com.bumptech.glide.request.Request;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LifecycleRequestManagerTest {
    private LifecycleRequestManager manager;

    @Before
    public void setUp() {
        manager = new LifecycleRequestManager();
    }

    @Test
    public void testCancelsRequestsOnStop() {
        Request request = mock(Request.class);
        manager.addRequest(request);
        manager.onStop();

        verify(request).clear();
    }

    @Test
    public void testCanAddAndRemoveRequest() {
        Request request = mock(Request.class);
        manager.addRequest(request);
        manager.removeRequest(request);

        manager.onStop();

        verify(request, never()).clear();
    }

    @Test
    public void testCanAddMultipleRequests() {
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
        Request request = mock(Request.class);
        when(request.isComplete()).thenReturn(true);
        manager.addRequest(request);

        manager.onStop();

        verify(request, never()).clear();
    }

    @Test
    public void testDoesNotClearFailedRequestsOnStop() {
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
}
