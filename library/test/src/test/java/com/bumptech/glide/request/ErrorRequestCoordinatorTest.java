package com.bumptech.glide.request;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class ErrorRequestCoordinatorTest {

  @Mock private Request primary;
  @Mock private Request error;
  @Mock private RequestCoordinator parent;
  private ErrorRequestCoordinator coordinator;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    coordinator = newCoordinator();
    coordinator.setRequests(primary, error);
  }

  @Test
  public void begin_startsPrimary() {
    coordinator.begin();
    verify(primary).begin();
  }

  @Test
  public void begin_whenPrimaryIsAlreadyRunning_doesNotStartPrimaryAgain() {
    coordinator.begin();
    coordinator.begin();
    verify(primary, times(1)).begin();
  }

  @Test
  public void clear_whenPrimaryHasNotFailed_clearsPrimary() {
    coordinator.clear();
    verify(primary).clear();
  }

  @Test
  public void clear_whenPrimaryHasNotFailed_doesNotClearError() {
    coordinator.clear();
    verify(error, never()).clear();
  }

  @Test
  public void clear_whenPrimaryHasFailed_errorIsRunning_clearsError() {
    coordinator.onRequestFailed(primary);
    coordinator.clear();
    verify(error).clear();
  }

  @Test
  public void clear_whenPrimaryHasFailed_clearsPrimary() {
    coordinator.onRequestFailed(primary);
    coordinator.clear();
    verify(primary).clear();
  }

  @Test
  public void clear_whenErrorIsRunning_clearsError() {
    coordinator.onRequestFailed(primary);
    coordinator.clear();

    verify(error).clear();
  }

  @Test
  public void pause_whenPrimaryIsRunning_pausesPrimary() {
    coordinator.begin();
    coordinator.pause();

    verify(primary).pause();
  }

  @Test
  public void pause_whenPrimaryIsComplete_doesNotPausePrimary() {
    coordinator.onRequestSuccess(primary);
    coordinator.pause();

    verify(primary, never()).pause();
  }

  @Test
  public void pause_whenPrimaryIsFailed_doesNotPausePrimary() {
    coordinator.onRequestFailed(primary);
    coordinator.pause();

    verify(primary, never()).pause();
  }

  @Test
  public void pause_whenErrorIsNotRunning_doesNotPauseError() {
    coordinator.pause();

    verify(error, never()).pause();
  }

  @Test
  public void pause_whenErrorIsComplete_doesNotPauseError() {
    coordinator.onRequestSuccess(error);
    coordinator.pause();

    verify(error, never()).pause();
  }

  @Test
  public void pause_whenErrorIsFailed_doesNotPauseError() {
    coordinator.onRequestFailed(error);
    coordinator.pause();

    verify(error, never()).pause();
  }

  @Test
  public void pause_whenErrorIsRunning_pausesError() {
    coordinator.onRequestFailed(primary);
    coordinator.pause();

    verify(error).pause();
  }

  @Test
  public void isRunning_primaryNotFailed_primaryNotRunning_returnsFalse() {
    assertThat(coordinator.isRunning()).isFalse();
  }

  @Test
  public void isRunning_primaryNotFailed_primaryRunning_returnsTrue() {
    coordinator.begin();
    assertThat(coordinator.isRunning()).isTrue();
  }

  @Test
  public void isRunning_primaryFailed_returnsTrue() {
    coordinator.onRequestFailed(primary);
    // A failed primary request starts the error request.
    assertThat(coordinator.isRunning()).isTrue();
  }

  @Test
  public void isComplete_primaryNotFailed_primaryNotComplete_returnsFalse() {
    assertThat(coordinator.isComplete()).isFalse();
  }

  @Test
  public void isComplete_primaryNotFailed_primaryComplete_returnsTrue() {
    coordinator.onRequestSuccess(primary);
    assertThat(coordinator.isComplete()).isTrue();
  }

  @Test
  public void isComplete_primaryFailed_errorNotComplete_returnsFalse() {
    coordinator.onRequestFailed(primary);
    assertThat(coordinator.isComplete()).isFalse();
  }

  @Test
  public void isComplete_primaryFailed_errorComplete_returnsTrue() {
    coordinator.onRequestFailed(primary);
    coordinator.onRequestSuccess(error);
    assertThat(coordinator.isComplete()).isTrue();
  }

  @Test
  public void isCleared_primaryNotFailed_primaryNotCancelled_returnsFalse() {
    coordinator.begin();
    assertThat(coordinator.isCleared()).isFalse();
  }

  @Test
  public void isCleared_primaryNotFailed_primaryCancelled_returnsTrue() {
    coordinator.begin();
    coordinator.clear();
    assertThat(coordinator.isCleared()).isTrue();
  }

  @Test
  public void isCleared_primaryFailed_errorNotCancelled_returnsFalse() {
    coordinator.onRequestFailed(primary);
    assertThat(coordinator.isCleared()).isFalse();
  }

  @Test
  public void isCleared_primaryFailed_errorCancelled_returnsTrue() {
    coordinator.onRequestFailed(primary);
    coordinator.clear();
    assertThat(coordinator.isCleared()).isTrue();
  }

  @Test
  public void isEquivalentTo() {
    assertThat(coordinator.isEquivalentTo(primary)).isFalse();

    ErrorRequestCoordinator other = newCoordinator(/* parent= */ null);
    assertThat(coordinator.isEquivalentTo(other)).isFalse();

    other.setRequests(primary, primary);
    assertThat(coordinator.isEquivalentTo(other)).isFalse();

    other.setRequests(error, error);
    assertThat(coordinator.isEquivalentTo(other)).isFalse();

    when(primary.isEquivalentTo(primary)).thenReturn(true);
    when(error.isEquivalentTo(error)).thenReturn(true);
    other.setRequests(primary, error);
    assertThat(coordinator.isEquivalentTo(other)).isTrue();

    other = newCoordinator(parent);
    other.setRequests(primary, error);
    assertThat(coordinator.isEquivalentTo(other)).isTrue();

    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    other = newCoordinator(parent);
    other.setRequests(primary, error);
    assertThat(coordinator.isEquivalentTo(other)).isTrue();
  }

  @Test
  public void canSetImage_withNotFailedPrimary_andNullParent_returnsTrue() {
    assertThat(coordinator.canSetImage(primary)).isTrue();
  }

  @Test
  public void canSetImage_withNotFailedPrimary_parentCanSetImage_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canSetImage(coordinator)).thenReturn(true);

    assertThat(coordinator.canSetImage(primary)).isTrue();
  }

  @Test
  public void canSetImage_withNotFailedPrimary_parentCanNotSetImage_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);

    assertThat(coordinator.canSetImage(primary)).isFalse();
  }

  @Test
  public void canSetImage_withError_andFailedPrimary_nullParent_returnsTrue() {
    coordinator.onRequestFailed(primary);
    assertThat(coordinator.canSetImage(error)).isTrue();
  }

  @Test
  public void canSetImage_withError_andFailedPrimary_nonNullParentCanSetImage_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canSetImage(coordinator)).thenReturn(true);
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canSetImage(error)).isTrue();
  }

  @Test
  public void canSetImage_withError_andFailedPrimary_nonNullParentCanNotSetImage_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canSetImage(error)).isFalse();
  }

  @Test
  public void canNotifyStatusChanged_withNotFailedPrimary_nullParent_returnsTrue() {
    assertThat(coordinator.canNotifyStatusChanged(primary)).isTrue();
  }

  @Test
  public void canNotifyStatusChanged_withNotFailedPrimary_nonNullParentCantNotify_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);

    assertThat(coordinator.canNotifyStatusChanged(primary)).isFalse();
  }

  @Test
  public void canNotifyStatusChanged_withNotFailedPrimary_nonNullParentCanNotify_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyStatusChanged(coordinator)).thenReturn(true);

    assertThat(coordinator.canNotifyStatusChanged(primary)).isTrue();
  }

  @Test
  public void canNotifyStatusChanged_withError_notFailedPrimary_nullParent_returnsFalse() {
    assertThat(coordinator.canNotifyStatusChanged(error)).isFalse();
  }

  @Test
  public void
      canNotifyStatusChanged_withErrorRequest_failedPrimary_nullParent_errorIsNotFailed_returnsFalse() {
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canNotifyStatusChanged(error)).isFalse();
  }

  @Test
  public void
      canNotifyStatusChanged_withErrorRequest_failedPrimary_nullParent_failedError_returnsTrue() {
    coordinator.onRequestFailed(primary);
    coordinator.onRequestFailed(error);

    assertThat(coordinator.canNotifyStatusChanged(error)).isTrue();
  }

  @Test
  public void canNotifyStatusChanged_withError_failedPrimary_nonNullParentCantNotify_false() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canNotifyStatusChanged(error)).isFalse();
  }

  @Test
  public void
      canNotifyStatusChanged_withError_failedPrimary_notFailedError_nonNullParentCanNotify_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    coordinator.onRequestFailed(primary);
    when(parent.canNotifyStatusChanged(coordinator)).thenReturn(true);

    assertThat(coordinator.canNotifyStatusChanged(error)).isFalse();
  }

  @Test
  public void
      canNotifyStatusChanged_withError_failedPrimary_failedError_nonNullParentCanNotify_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    coordinator.onRequestFailed(primary);
    when(parent.canNotifyStatusChanged(coordinator)).thenReturn(true);
    coordinator.onRequestFailed(error);

    assertThat(coordinator.canNotifyStatusChanged(error)).isTrue();
  }

  @Test
  public void isAnyResourceSet_primaryNotSet_nullParent_returnsFalse() {
    assertThat(coordinator.isAnyResourceSet()).isFalse();
  }

  @Test
  public void isAnyResourceSet_primarySet_nullParent_returnsTrue() {
    when(primary.isAnyResourceSet()).thenReturn(true);
    coordinator.onRequestSuccess(primary);
    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_primarySet_parentResourceNotSet_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(primary.isAnyResourceSet()).thenReturn(true);
    coordinator.onRequestSuccess(primary);

    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_primarySet_parentSet_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(primary.isAnyResourceSet()).thenReturn(true);
    coordinator.onRequestSuccess(primary);
    when(parent.isAnyResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_parentSet_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.isAnyResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isFalse();
  }

  @Test
  public void isAnyResourceSet_errorSet_failedPrimary_nullParent_returnsTrue() {
    coordinator.onRequestFailed(primary);
    when(error.isAnyResourceSet()).thenReturn(true);
    coordinator.onRequestSuccess(error);
    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_errorSet_failedPrimary_nonNullParentNotSet_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    coordinator.onRequestFailed(primary);
    when(error.isAnyResourceSet()).thenReturn(true);
    coordinator.onRequestSuccess(error);

    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_errorSet_nonNullParentSet_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.isAnyResourceSet()).thenReturn(true);
    when(error.isAnyResourceSet()).thenReturn(true);
    coordinator.onRequestSuccess(error);

    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_primaryNotSet_errorNotSet_nonNullParentNotSet_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);

    assertThat(coordinator.isAnyResourceSet()).isFalse();
  }

  @Test
  public void isAnyResourceSet_primaryNotSet_errorNotSet_nonNullParentSet_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);

    when(parent.isAnyResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isFalse();
  }

  @Test
  public void onRequestSuccess_nullParent_doesNotThrow() {
    coordinator.onRequestSuccess(primary);
  }

  @Test
  public void onRequestSuccess_nonNullParent_callsParent() {
    coordinator = newCoordinator(parent);
    coordinator.onRequestSuccess(primary);
    verify(parent).onRequestSuccess(coordinator);
  }

  @Test
  public void onRequestFailed_primaryRequest_notRunningError_beingsError() {
    coordinator.onRequestFailed(primary);
    verify(error).begin();
  }

  @Test
  public void onRequestFailed_errorRequest_doesNotBeginError() {
    coordinator.onRequestFailed(error);
    verify(error, never()).begin();
  }

  @Test
  public void onRequestFailed_primaryRequest_notRunningError_nonNullParent_doesNotNotifyParent() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);

    coordinator.onRequestFailed(primary);
    verify(parent, never()).onRequestFailed(any(Request.class));
  }

  @Test
  public void onRequestFailed_errorRequest_nonNullParent_notifiesParent() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);

    coordinator.onRequestFailed(error);

    verify(parent).onRequestFailed(coordinator);
  }

  @Test
  public void onRequestFailed_primaryRequest_runningError_nonNullParent_doesNotNotifyParent() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    coordinator.onRequestFailed(primary);

    coordinator.onRequestFailed(primary);

    verify(parent, never()).onRequestFailed(any(Request.class));
  }

  @Test
  public void canNotifyCleared_primaryRequest_nullParent_returnsTrue() {
    assertThat(coordinator.canNotifyCleared(primary)).isTrue();
  }

  @Test
  public void canNotifyCleared_primaryRequest_parentCanNotNotify_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);

    assertThat(coordinator.canNotifyCleared(primary)).isFalse();
  }

  @Test
  public void canNotifyCleared_primaryRequest_parentCanNotify_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyCleared(coordinator)).thenReturn(true);

    assertThat(coordinator.canNotifyCleared(primary)).isTrue();
  }

  @Test
  public void canNotifyCleared_primaryRequestFailed_parentCanNotify_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyCleared(coordinator)).thenReturn(true);
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canNotifyCleared(primary)).isTrue();
  }

  @Test
  public void canNotifyCleared_primaryRequestFailed_parentCanNotNotify_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canNotifyCleared(primary)).isFalse();
  }

  @Test
  public void canNotifyCleared_primaryRequestFailed_nullParent_returnsTrue() {
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canNotifyCleared(primary)).isTrue();
  }

  @Test
  public void canNotifyCleared_errorRequest_nullParent_returnsFalse() {
    assertThat(coordinator.canNotifyCleared(error)).isFalse();
  }

  @Test
  public void canNotifyCleared_errorRequest_primaryFailed_nullParent_returnsFalse() {
    coordinator.onRequestFailed(primary);
    assertThat(coordinator.canNotifyCleared(error)).isFalse();
  }

  @Test
  public void
      canNotifyCleared_primaryRequest_primaryFailed_nonNullParentCanNotNotify_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyCleared(coordinator)).thenReturn(false);
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canNotifyCleared(primary)).isFalse();
  }

  @Test
  public void canNotifyCleared_errorRequest_primaryFailed_nonNullParentCanNotNotify_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyCleared(coordinator)).thenReturn(false);
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canNotifyCleared(error)).isFalse();
  }

  @Test
  public void canNotifyCleared_errorRequest_primaryFailed_nonNullParentCanNotify_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyCleared(coordinator)).thenReturn(true);
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canNotifyCleared(error)).isFalse();
  }

  @Test
  public void canNotifyCleared_primaryRequest_primaryFailed_nonNullParentCanNotify_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyCleared(coordinator)).thenReturn(true);
    coordinator.onRequestFailed(primary);

    assertThat(coordinator.canNotifyCleared(primary)).isTrue();
  }

  private static ErrorRequestCoordinator newCoordinator() {
    return newCoordinator(/* parent= */ null);
  }

  private static ErrorRequestCoordinator newCoordinator(@Nullable RequestCoordinator parent) {
    return new ErrorRequestCoordinator(/* requestLock= */ new Object(), parent);
  }
}
