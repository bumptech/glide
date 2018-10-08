package com.bumptech.glide.manager;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bumptech.glide.request.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RequestTrackerTest {
  private RequestTracker tracker;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    tracker = new RequestTracker();
  }

  @Test
  public void clearRequests_doesNotRecycleRequests() {
    FakeRequest request = new FakeRequest();
    tracker.addRequest(request);

    tracker.clearRequests();

    assertThat(request.isCleared()).isTrue();
    assertThat(request.isRecycled()).isFalse();
  }

  @Test
  public void clearRemoveAndRecycle_withRequestPreviouslyClearedInClearRequests_doesNothing() {
    FakeRequest request = new FakeRequest();
    tracker.addRequest(request);

    tracker.clearRequests();
    tracker.clearRemoveAndRecycle(request);

    assertThat(request.isCleared()).isTrue();
    assertThat(request.isRecycled()).isFalse();
  }

  @Test
  public void clearRemoveAndRecycle_withNullRequest_doesNothingAndReturnsTrue() {
    assertThat(tracker.clearRemoveAndRecycle(null)).isTrue();
  }

  @Test
  public void clearRemoveAndRecycle_withUnTrackedRequest_doesNothingAndReturnsFalse() {
    FakeRequest request = new FakeRequest();

    assertThat(tracker.clearRemoveAndRecycle(request)).isFalse();

    assertThat(request.isCleared()).isFalse();
    assertThat(request.isRecycled()).isFalse();
  }

  @Test
  public void clearRemoveAndRecycle_withTrackedRequest_clearsRecyclesAndReturnsTrue() {
    FakeRequest request = new FakeRequest();
    tracker.addRequest(request);

    assertThat(tracker.clearRemoveAndRecycle(request)).isTrue();
    assertThat(request.isCleared()).isTrue();
    assertThat(request.isRecycled()).isTrue();
  }

  @Test
  public void clearRemoveAndRecycle_withAlreadyRemovedRequest_doesNothingAndReturnsFalse() {
    FakeRequest request = new FakeRequest();
    tracker.addRequest(request);
    tracker.clearRemoveAndRecycle(request);
    assertThat(tracker.clearRemoveAndRecycle(request)).isFalse();

    assertThat(request.isCleared()).isTrue();
    assertThat(request.isRecycled()).isTrue();
  }

  @Test
  public void clearRequests_withPreviouslyClearedRequest_doesNotClearRequestAgain() {
    FakeRequest request = new FakeRequest();
    tracker.addRequest(request);
    tracker.clearRemoveAndRecycle(request);

    tracker.clearRequests();

    assertThat(request.isCleared()).isTrue();
  }

  @Test
  public void clearRequests_withMultipleRequests_clearsAllRequests() {
    FakeRequest first = new FakeRequest();
    FakeRequest second = new FakeRequest();
    tracker.addRequest(first);
    tracker.addRequest(second);

    tracker.clearRequests();

    assertThat(first.isCleared()).isTrue();
    assertThat(second.isCleared()).isTrue();
  }

  @Test
  public void pauseRequest_withRunningRequest_pausesRequest() {
    FakeRequest request = new FakeRequest();
    request.setIsRunning();
    tracker.addRequest(request);

    tracker.pauseRequests();

    assertThat(request.isCleared()).isTrue();
  }

  @Test
  public void pauseRequests_withCompletedRequest_doesNotClearRequest() {
    FakeRequest request = new FakeRequest();
    tracker.addRequest(request);

    request.setIsComplete();
    tracker.pauseRequests();

    assertThat(request.isCleared()).isFalse();
  }

  @Test
  public void runRequest_startsRequest() {
    FakeRequest request = new FakeRequest();
    tracker.runRequest(request);

    assertThat(request.isRunning()).isTrue();
  }

  @Test
  public void runRequest_whenPaused_doesNotStartRequest() {
    FakeRequest request = new FakeRequest();
    tracker.pauseRequests();
    tracker.runRequest(request);

    assertThat(request.isRunning()).isFalse();
  }

  @Test
  public void runRequest_withAllRequestsPaused_doesNotStartRequest() {
    FakeRequest request = new FakeRequest();
    tracker.pauseAllRequests();
    tracker.runRequest(request);

    assertThat(request.isRunning()).isFalse();
  }

  @Test
  public void runRequest_afterPausingAndResuming_startsRequest() {
    FakeRequest request = new FakeRequest();
    tracker.pauseRequests();
    tracker.runRequest(request);
    tracker.resumeRequests();

    assertThat(request.isRunning()).isTrue();
  }

  @Test
  public void pauseRequests_withFailedRequest_doesNotClearRequest() {
    FakeRequest request = new FakeRequest();
    request.setIsFailed();
    tracker.addRequest(request);

    tracker.pauseRequests();

    assertThat(request.isCleared()).isFalse();
  }

  @Test
  public void resumeRequests_withRequestAddedWhilePaused_startsRequest() {
    FakeRequest request = new FakeRequest();
    tracker.addRequest(request);

    tracker.resumeRequests();

    assertThat(request.isRunning()).isTrue();
  }

  @Test
  public void resumeRequests_withCompletedRequest_doesNotRestartCompletedRequest() {
    FakeRequest request = new FakeRequest();
    request.setIsComplete();
    tracker.addRequest(request);

    tracker.resumeRequests();

    assertThat(request.isRunning()).isFalse();
  }

  @Test
  public void resumeRequests_withFailedRequest_restartsRequest() {
    FakeRequest request = new FakeRequest();
    request.setIsFailed();
    tracker.addRequest(request);

    tracker.resumeRequests();

    assertThat(request.isRunning()).isTrue();
  }

  @Test
  public void addRequest_withRunningRequest_doesNotRestartRequest() {
    FakeRequest request = new FakeRequest();
    request.setIsRunning();
    tracker.addRequest(request);

    tracker.resumeRequests();

    assertThat(request.isRunning()).isTrue();
  }

  @Test
  public void resumeRequests_withRequestThatClearsAnotherRequest_avoidsConcurrentModifications() {
    Request first = mock(Request.class);
    Request second = mock(Request.class);

    doAnswer(new ClearAndRemoveRequest(second)).when(first).begin();

    tracker.addRequest(mock(Request.class));
    tracker.addRequest(first);
    tracker.addRequest(second);

    tracker.resumeRequests();
  }

  @Test
  public void pauseRequests_withRequestThatClearsAnother_avoidsConcurrentModifications() {
    Request first = mock(Request.class);
    Request second = mock(Request.class);

    when(first.isRunning()).thenReturn(true);
    doAnswer(new ClearAndRemoveRequest(second)).when(first).clear();

    tracker.addRequest(mock(Request.class));
    tracker.addRequest(first);
    tracker.addRequest(second);

    tracker.pauseRequests();
  }

  @Test
  public void clearRequests_withRequestThatClearsAnother_avoidsConcurrentModifications() {
    Request first = mock(Request.class);
    Request second = mock(Request.class);

    doAnswer(new ClearAndRemoveRequest(second)).when(first).clear();

    tracker.addRequest(mock(Request.class));
    tracker.addRequest(first);
    tracker.addRequest(second);

    tracker.clearRequests();
  }

  @Test
  public void restartRequests_withRequestThatClearsAnother_avoidsConcurrentModifications() {
    Request first = mock(Request.class);
    Request second = mock(Request.class);

    doAnswer(new ClearAndRemoveRequest(second)).when(first).clear();

    tracker.addRequest(mock(Request.class));
    tracker.addRequest(first);
    tracker.addRequest(second);

    tracker.restartRequests();
  }

  @Test
  public void restartRequests_withFailedRequest_restartsRequest() {
    FakeRequest request = new FakeRequest();
    request.setIsFailed();
    tracker.addRequest(request);

    tracker.restartRequests();

    assertThat(request.isRunning()).isTrue();
  }

  @Test
  public void restartRequests_withIncompleteRequest_restartsRequest() {
    FakeRequest request = new FakeRequest();
    tracker.addRequest(request);

    tracker.restartRequests();

    assertThat(request.isRunning()).isTrue();
  }

  @Test
  public void restartRequests_whenPaused_doesNotRestartRequests() {
    FakeRequest request = new FakeRequest();
    request.setIsFailed();
    tracker.pauseRequests();
    tracker.addRequest(request);

    tracker.restartRequests();

    assertThat(request.isRunning()).isFalse();
  }

  @Test
  public void restartRequests_withFailedRequestAddedWhilePaused_clearsFailedRequest() {
    FakeRequest request = new FakeRequest();
    request.setIsFailed();

    tracker.pauseRequests();
    tracker.addRequest(request);

    tracker.restartRequests();
    assertThat(request.isCleared()).isTrue();
  }

  @Test
  public void restartRequests_withIncompleteRequestAddedWhilePaused_doesNotRestartRequest() {
    FakeRequest request = new FakeRequest();

    tracker.pauseRequests();
    tracker.addRequest(request);
    tracker.restartRequests();

    assertThat(request.isRunning()).isFalse();
  }

  @Test
  public void restartRequests_withIncompleteRequestAddedWhilePaused_clearsRequestOnRestart() {
    FakeRequest request = new FakeRequest();

    tracker.pauseRequests();
    tracker.addRequest(request);
    tracker.restartRequests();

    assertThat(request.isCleared()).isTrue();
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

  @Test
  public void testPauseAllRequests_returnsTrueFromIsPaused() {
    tracker.pauseAllRequests();
    assertTrue(tracker.isPaused());
  }

  @Test
  public void resumeRequests_afterRequestIsPausedViaPauseAllRequests_resumesRequest() {
    FakeRequest request = new FakeRequest();
    request.setIsComplete();

    tracker.addRequest(request);
    tracker.pauseAllRequests();

    assertThat(request.isCleared()).isTrue();

    // reset complete status.
    request.setIsComplete(false);
    tracker.resumeRequests();

    assertThat(request.isRunning()).isTrue();
  }

  private static final class FakeRequest implements Request {
    private boolean isRunning;
    private boolean isFailed;
    private boolean isCleared;
    private boolean isComplete;
    private boolean isRecycled;

    void setIsComplete() {
      setIsComplete(true);
    }

    void setIsComplete(boolean isComplete) {
      this.isComplete = isComplete;
    }

    void setIsFailed() {
      isFailed = true;
    }

    void setIsRunning() {
      isRunning = true;
    }

    boolean isRecycled() {
      return isRecycled;
    }

    @Override
    public void begin() {
      if (isRunning) {
        throw new IllegalStateException();
      }
      isRunning = true;
    }

    @Override
    public void clear() {
      if (isCleared) {
        throw new IllegalStateException();
      }
      isRunning = false;
      isFailed = false;
      isCleared = true;
    }

    @Override
    public boolean isRunning() {
      return isRunning;
    }

    @Override
    public boolean isComplete() {
      return isComplete;
    }

    @Override
    public boolean isResourceSet() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCleared() {
      return isCleared;
    }

    @Override
    public boolean isFailed() {
      return isFailed;
    }

    @Override
    public void recycle() {
      if (isRecycled) {
        throw new IllegalStateException();
      }
      isRecycled = true;
    }

    @Override
    public boolean isEquivalentTo(Request other) {
      throw new UnsupportedOperationException();
    }
  }

  private class ClearAndRemoveRequest implements Answer<Void> {

    private final Request toRemove;

    ClearAndRemoveRequest(Request toRemove) {
      this.toRemove = toRemove;
    }

    @Override
    public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
      tracker.clearRemoveAndRecycle(toRemove);
      return null;
    }
  }
}
