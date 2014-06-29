package com.bumptech.glide.manager;

import com.bumptech.glide.request.Request;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestTrackerTest {
    private RequestTracker tracker;

    @Before
    public void setUp() {
        tracker = new RequestTracker();
    }

    @Test
    public void testClearRequestsClearsAddedRequests() {
        Request request = mock(Request.class);
        tracker.addRequest(request);

        tracker.clearRequests();

        verify(request).clear();
    }

    @Test
    public void testCanAddAndRemoveRequest() {
        Request request = mock(Request.class);
        tracker.addRequest(request);
        tracker.removeRequest(request);

        tracker.clearRequests();

        verify(request, never()).clear();
    }

    @Test
    public void testCanAddMultipleRequests() {
        Request first = mock(Request.class);
        Request second = mock(Request.class);
        tracker.addRequest(first);
        tracker.addRequest(second);

        tracker.clearRequests();

        verify(first).clear();
        verify(second).clear();
    }

    @Test
    public void testClearsInProgressRequestsOnPause() {
        Request request = mock(Request.class);
        when(request.isRunning()).thenReturn(true);
        tracker.addRequest(request);

        tracker.pauseRequests();

        verify(request).clear();
    }

    @Test
    public void testDoesNotClearCompleteRequestsOnPause() {
        Request request = mock(Request.class);
        when(request.isComplete()).thenReturn(true);
        tracker.addRequest(request);

        tracker.pauseRequests();

        verify(request, never()).clear();
    }

    @Test
    public void testDoesNotClearFailedRequestsOnPause() {
        Request request = mock(Request.class);
        when(request.isFailed()).thenReturn(true);
        tracker.addRequest(request);

        tracker.pauseRequests();

        verify(request, never()).clear();
    }

    @Test
    public void testRestartsStoppedRequestOnResume() {
        Request request = mock(Request.class);
        tracker.addRequest(request);

        tracker.resumeRequests();

        verify(request).begin();
    }

    @Test
    public void testDoesNotRestartCompletedRequestsOnResume() {
        Request request = mock(Request.class);
        when(request.isComplete()).thenReturn(true);
        tracker.addRequest(request);

        tracker.resumeRequests();

        verify(request, never()).begin();
    }

    @Test
    public void testDoesRestartFailedRequestsOnResume() {
        Request request = mock(Request.class);
        when(request.isFailed()).thenReturn(true);
        tracker.addRequest(request);

        tracker.resumeRequests();

        verify(request).begin();
    }

    @Test
    public void testDoesNotStartStartedRequestsOnResume() {
        Request request = mock(Request.class);
        when(request.isRunning()).thenReturn(true);
        tracker.addRequest(request);

        tracker.resumeRequests();

        verify(request, never()).begin();
    }

    @Test
    public void testRestartsFailedRequestRestart() {
        Request request = mock(Request.class);
        when(request.isFailed()).thenReturn(true);
        tracker.addRequest(request);

        tracker.restartRequests();

        verify(request).begin();
    }

    @Test
    public void testCancelsAndRestartsNotYetFinishedRequestsOnRestart() {
        Request request = mock(Request.class);
        when(request.isComplete()).thenReturn(false);
        tracker.addRequest(request);

        tracker.restartRequests();

        verify(request).clear();
        verify(request).begin();
    }
}