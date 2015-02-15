package com.bumptech.glide.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.request.Request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(JUnit4.class)
public class RequestTrackerTest {
  private RequestTracker tracker;

  @Before
  public void setUp() {
    tracker = new RequestTracker();
  }

  @Test
  public void testClearsAddedRequestsOnDestroy() {
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
  public void testPausesInProgressRequestsWhenPaused() {
    Request request = mock(Request.class);
    when(request.isRunning()).thenReturn(true);
    tracker.addRequest(request);

    tracker.pauseRequests();

    verify(request).pause();
  }

  @Test
  public void testDoesNotClearCompleteRequestsWhenPaused() {
    Request request = mock(Request.class);
    tracker.addRequest(request);

    when(request.isComplete()).thenReturn(true);
    tracker.pauseRequests();

    verify(request, never()).clear();
  }

  @Test
  public void testStartsRequestOnRun() {
    Request request = mock(Request.class);
    tracker.runRequest(request);

    verify(request).begin();
  }

  @Test
  public void testDoesNotStartRequestOnRunIfPaused() {
    Request request = mock(Request.class);
    tracker.pauseRequests();
    tracker.runRequest(request);

    verify(request, never()).begin();
  }

  @Test
  public void testStartsRequestAddedWhenPausedWhenResumed() {
    Request request = mock(Request.class);
    tracker.pauseRequests();
    tracker.runRequest(request);
    tracker.resumeRequests();

    verify(request).begin();
  }

  @Test
  public void testDoesNotClearFailedRequestsWhenPaused() {
    Request request = mock(Request.class);
    when(request.isFailed()).thenReturn(true);
    tracker.addRequest(request);

    tracker.pauseRequests();

    verify(request, never()).clear();
  }

  @Test
  public void testRestartsStoppedRequestWhenResumed() {
    Request request = mock(Request.class);
    tracker.addRequest(request);

    tracker.resumeRequests();

    verify(request).begin();
  }

  @Test
  public void testDoesNotRestartCompletedRequestsWhenResumed() {
    Request request = mock(Request.class);
    when(request.isComplete()).thenReturn(true);
    tracker.addRequest(request);

    tracker.resumeRequests();

    verify(request, never()).begin();
  }

  @Test
  public void testDoesRestartFailedRequestsWhenResumed() {
    Request request = mock(Request.class);
    when(request.isFailed()).thenReturn(true);
    tracker.addRequest(request);

    tracker.resumeRequests();

    verify(request).begin();
  }

  @Test
  public void testDoesNotStartStartedRequestsWhenResumed() {
    Request request = mock(Request.class);
    when(request.isRunning()).thenReturn(true);
    tracker.addRequest(request);

    tracker.resumeRequests();

    verify(request, never()).begin();
  }

  @Test
  public void testAvoidsConcurrentModificationWhenResuming() {
    Request first = mock(Request.class);
    Request second = mock(Request.class);

    doAnswer(new RemoveRequest(second)).when(first).begin();

    tracker.addRequest(mock(Request.class));
    tracker.addRequest(first);
    tracker.addRequest(second);

    tracker.resumeRequests();
  }

  @Test
  public void testAvoidsConcurrentModificationWhenPausing() {
    Request first = mock(Request.class);
    Request second = mock(Request.class);

    when(first.isRunning()).thenReturn(true);
    doAnswer(new RemoveRequest(second)).when(first).pause();

    tracker.addRequest(mock(Request.class));
    tracker.addRequest(first);
    tracker.addRequest(second);

    tracker.pauseRequests();
  }

  @Test
  public void testAvoidsConcurrentModificationWhenClearing() {
    Request first = mock(Request.class);
    Request second = mock(Request.class);

    doAnswer(new RemoveRequest(second)).when(first).clear();

    tracker.addRequest(mock(Request.class));
    tracker.addRequest(first);
    tracker.addRequest(second);

    tracker.clearRequests();
  }

  @Test
  public void testAvoidsConcurrentModificationWhenRestarting() {
    Request first = mock(Request.class);
    Request second = mock(Request.class);

    doAnswer(new RemoveRequest(second)).when(first).pause();

    tracker.addRequest(mock(Request.class));
    tracker.addRequest(first);
    tracker.addRequest(second);

    tracker.restartRequests();
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
  public void testPausesAndRestartsNotYetFinishedRequestsOnRestart() {
    Request request = mock(Request.class);
    when(request.isComplete()).thenReturn(false);
    tracker.addRequest(request);

    tracker.restartRequests();

    verify(request).pause();
    verify(request).begin();
  }

  @Test
  public void testDoesNotBeginFailedRequestOnRestartIfPaused() {
    Request request = mock(Request.class);
    when(request.isFailed()).thenReturn(true);
    tracker.pauseRequests();
    tracker.addRequest(request);

    tracker.restartRequests();

    verify(request, never()).begin();
  }

  @Test
  public void testPausesFailedRequestOnRestartIfPaused() {
    Request request = mock(Request.class);
    when(request.isFailed()).thenReturn(true);
    tracker.pauseRequests();
    tracker.addRequest(request);

    tracker.restartRequests();
    verify(request).pause();
  }

  @Test
  public void testDoesNotBeginIncompleteRequestsOnRestartIfPaused() {
    Request request = mock(Request.class);
    when(request.isFailed()).thenReturn(false);
    when(request.isComplete()).thenReturn(false);
    tracker.pauseRequests();
    tracker.addRequest(request);
    tracker.restartRequests();

    verify(request, never()).begin();
  }

  @Test
  public void testPausesIncompleteRequestsOnRestartIfPaused() {
    Request request = mock(Request.class);
    when(request.isFailed()).thenReturn(false);
    when(request.isComplete()).thenReturn(false);
    tracker.pauseRequests();
    tracker.addRequest(request);
    tracker.restartRequests();

    verify(request).pause();
  }

  @Test
  public void testReturnsTrueFromIsPausedWhenPaused() {
    tracker.pauseRequests();
    assertTrue(tracker.isPaused());
  }

  @Test
  public void testReturnsFalseFromIsPausedWhenResumed() {
    tracker.resumeRequests();
    assertFalse(tracker.isPaused());
  }

  private class RemoveRequest implements Answer<Void> {

    private Request toRemove;

    public RemoveRequest(Request toRemove) {
      this.toRemove = toRemove;
    }

    @Override
    public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
      tracker.removeRequest(toRemove);
      return null;
    }
  }
}