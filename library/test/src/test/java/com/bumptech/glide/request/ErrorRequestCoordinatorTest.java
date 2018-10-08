package com.bumptech.glide.request;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    coordinator = new ErrorRequestCoordinator(/*parent=*/ null);
    coordinator.setRequests(primary, error);
  }

  @Test
  public void begin_startsPrimary() {
    coordinator.begin();
    verify(primary).begin();
  }

  @Test
  public void begin_whenPrimaryIsAlreadyRunning_doesNotStartPrimaryAgain() {
    when(primary.isRunning()).thenReturn(true);
    coordinator.begin();
    verify(primary, never()).begin();
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
    when(primary.isFailed()).thenReturn(true);
    when(error.isRunning()).thenReturn(true);
    coordinator.clear();
    verify(error).clear();
  }

  @Test
  public void clear_whenPrimaryHasFailed_clearsPrimary() {
    when(primary.isFailed()).thenReturn(true);
    coordinator.clear();
    verify(primary).clear();
  }

  @Test
  public void clear_whenErrorIsRunning_clearsError() {
    when(error.isRunning()).thenReturn(true);
    coordinator.clear();

    verify(error).clear();
  }

  @Test
  public void isRunning_primaryNotFailed_primaryNotRunning_returnsFalse() {
    assertThat(coordinator.isRunning()).isFalse();
  }

  @Test
  public void isRunning_primaryNotFailed_primaryRunning_returnsTrue() {
    when(primary.isRunning()).thenReturn(true);
    assertThat(coordinator.isRunning()).isTrue();
  }

  @Test
  public void isRunning_primaryFailed_errorNotRunning_returnsFalse() {
    when(primary.isFailed()).thenReturn(true);
    assertThat(coordinator.isRunning()).isFalse();
  }

  @Test
  public void isRunning_primaryFailed_errorRunning_returnsTrue() {
    when(primary.isFailed()).thenReturn(true);
    when(error.isRunning()).thenReturn(true);
    assertThat(coordinator.isRunning()).isTrue();
  }

  @Test
  public void isComplete_primaryNotFailed_primaryNotComplete_returnsFalse() {
    assertThat(coordinator.isComplete()).isFalse();
  }

  @Test
  public void isComplete_primaryNotFailed_primaryComplete_returnsTrue() {
    when(primary.isComplete()).thenReturn(true);
    assertThat(coordinator.isComplete()).isTrue();
  }

  @Test
  public void isComplete_primaryFailed_errorNotComplete_returnsFalse() {
    when(primary.isFailed()).thenReturn(true);
    assertThat(coordinator.isComplete()).isFalse();
  }

  @Test
  public void isComplete_primaryFailed_errorComplete_returnsTrue() {
    when(primary.isFailed()).thenReturn(true);
    when(error.isComplete()).thenReturn(true);
    assertThat(coordinator.isComplete()).isTrue();
  }

  @Test
  public void isResourceSet_primaryNotFailed_primaryNotResourceSet_returnsFalse() {
    assertThat(coordinator.isResourceSet()).isFalse();
  }

  @Test
  public void isResourceSet_primaryNotFailed_primaryResourceSet_returnsTrue() {
    when(primary.isResourceSet()).thenReturn(true);
    assertThat(coordinator.isResourceSet()).isTrue();
  }

  @Test
  public void isResourceSet_primaryFailed_errorNotResourceSet_returnsFalse() {
    when(primary.isFailed()).thenReturn(true);
    assertThat(coordinator.isResourceSet()).isFalse();
  }

  @Test
  public void isResourceSet_primaryFailed_errorResourceSet_returnsTrue() {
    when(primary.isFailed()).thenReturn(true);
    when(error.isResourceSet()).thenReturn(true);
    assertThat(coordinator.isResourceSet()).isTrue();
  }

  @Test
  public void isCancelled_primaryNotFailed_primaryNotCancelled_returnsFalse() {
    assertThat(coordinator.isCleared()).isFalse();
  }

  @Test
  public void isCancelled_primaryNotFailed_primaryCancelled_returnsTrue() {
    when(primary.isCleared()).thenReturn(true);
    assertThat(coordinator.isCleared()).isTrue();
  }

  @Test
  public void isCancelled_primaryFailed_errorNotCancelled_returnsFalse() {
    when(primary.isFailed()).thenReturn(true);
    assertThat(coordinator.isCleared()).isFalse();
  }

  @Test
  public void isCancelled_primaryFailed_errorCancelled_returnsTrue() {
    when(primary.isFailed()).thenReturn(true);
    when(error.isCleared()).thenReturn(true);
    assertThat(coordinator.isCleared()).isTrue();
  }

  @Test
  public void isFailed_primaryNotFailed_errorNotFailed_returnsFalse() {
    assertThat(coordinator.isFailed()).isFalse();
  }

  @Test
  public void isFailed_primaryFailed_errorNotFailed_returnsFalse() {
    when(primary.isFailed()).thenReturn(true);
    assertThat(coordinator.isFailed()).isFalse();
  }

  @Test
  public void isFailed_primaryNotFailed_errorFailed_returnsFalse() {
    when(error.isFailed()).thenReturn(true);
    assertThat(coordinator.isFailed()).isFalse();
  }

  @Test
  public void isFailed_primaryFailed_andErrorFailed_returnsTrue() {
    when(primary.isFailed()).thenReturn(true);
    when(error.isFailed()).thenReturn(true);
    assertThat(coordinator.isFailed()).isTrue();
  }

  @Test
  public void recycle_recyclesPrimaryAndError() {
    coordinator.recycle();
    verify(primary).recycle();
    verify(error).recycle();
  }

  @Test
  public void isEquivalentTo() {
    assertThat(coordinator.isEquivalentTo(primary)).isFalse();

    ErrorRequestCoordinator other = new ErrorRequestCoordinator(/*parent=*/ null);
    assertThat(coordinator.isEquivalentTo(other)).isFalse();

    other.setRequests(primary, primary);
    assertThat(coordinator.isEquivalentTo(other)).isFalse();

    other.setRequests(error, error);
    assertThat(coordinator.isEquivalentTo(other)).isFalse();

    when(primary.isEquivalentTo(primary)).thenReturn(true);
    when(error.isEquivalentTo(error)).thenReturn(true);
    other.setRequests(primary, error);
    assertThat(coordinator.isEquivalentTo(other)).isTrue();

    other = new ErrorRequestCoordinator(parent);
    other.setRequests(primary, error);
    assertThat(coordinator.isEquivalentTo(other)).isTrue();

    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    other = new ErrorRequestCoordinator(parent);
    other.setRequests(primary, error);
    assertThat(coordinator.isEquivalentTo(other)).isTrue();
  }

  @Test
  public void canSetImage_withNotFailedPrimary_andNullParent_returnsTrue() {
    assertThat(coordinator.canSetImage(primary)).isTrue();
  }

  @Test
  public void canSetImage_withError_andNullParent_andNotFailedPrimary_returnsFalse() {
    assertThat(coordinator.canSetImage(error)).isFalse();
  }

  @Test
  public void canSetImage_withNotFailedPrimary_parentCanSetImage_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canSetImage(coordinator)).thenReturn(true);

    assertThat(coordinator.canSetImage(primary)).isTrue();
  }

  @Test
  public void canSetImage_withNotFailedPrimary_parentCanNotSetImage_returnsFalse() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);

    assertThat(coordinator.canSetImage(primary)).isFalse();
  }

  @Test
  public void canSetImage_withError_andFailedPrimary_nullParent_returnsTrue() {
    when(primary.isFailed()).thenReturn(true);
    assertThat(coordinator.canSetImage(error)).isTrue();
  }

  @Test
  public void canSetImage_withError_andFailedPrimary_nonNullParentCanSetImage_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canSetImage(coordinator)).thenReturn(true);
    when(primary.isFailed()).thenReturn(true);

    assertThat(coordinator.canSetImage(error)).isTrue();
  }

  @Test
  public void canSetImage_withError_andFailedPrimary_nonNullParentCanNotSetImage_returnsFalse() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(primary.isFailed()).thenReturn(true);

    assertThat(coordinator.canSetImage(error)).isFalse();
  }

  @Test
  public void canNotifyStatusChanged_withNotFailedPrimary_nullParent_returnsTrue() {
    assertThat(coordinator.canNotifyStatusChanged(primary)).isTrue();
  }

  @Test
  public void canNotifyStatusChanged_withNotFailedPrimary_nonNullParentCantNotify_returnsFalse() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);

    assertThat(coordinator.canNotifyStatusChanged(primary)).isFalse();
  }

  @Test
  public void canNotifyStatusChanged_withNotFailedPrimary_nonNullParentCanNotify_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyStatusChanged(coordinator)).thenReturn(true);

    assertThat(coordinator.canNotifyStatusChanged(primary)).isTrue();
  }

  @Test
  public void canNotifyStatusChanged_withError_notFailedPrimary_nullParent_returnsFalse() {
    assertThat(coordinator.canNotifyStatusChanged(error)).isFalse();
  }

  @Test
  public void canNotifyStatusChanged_withError_failedPrimary_nullParent_returnsTrue() {
    when(primary.isFailed()).thenReturn(true);

    assertThat(coordinator.canNotifyStatusChanged(error)).isTrue();
  }

  @Test
  public void canNotifyStatusChanged_withError_failedPrimary_nonNullParentCantNotify_false() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(primary.isFailed()).thenReturn(true);

    assertThat(coordinator.canNotifyStatusChanged(error)).isFalse();
  }

  @Test
  public void canNotifyStatusChanged_withError_failedPrimary_nonNullParentCanNotify_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(primary.isFailed()).thenReturn(true);
    when(parent.canNotifyStatusChanged(coordinator)).thenReturn(true);

    assertThat(coordinator.canNotifyStatusChanged(primary)).isTrue();
  }

  @Test
  public void isAnyResourceSet_primaryNotSet_nullParent_returnsFalse() {
    assertThat(coordinator.isAnyResourceSet()).isFalse();
  }

  @Test
  public void isAnyResourceSet_primarySet_nullParent_returnsTrue() {
    when(primary.isResourceSet()).thenReturn(true);
    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_primarySet_parentResourceNotSet_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(primary.isResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_primarySet_parentSet_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(primary.isResourceSet()).thenReturn(true);
    when(parent.isAnyResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_parentSet_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.isAnyResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_errorSet_notFailedPrimary_nullParent_returnsFalse() {
    when(error.isResourceSet()).thenReturn(true);
    assertThat(coordinator.isAnyResourceSet()).isFalse();
  }

  @Test
  public void isAnyResourceSet_errorSet_failedPrimary_nullParent_returnsTrue() {
    when(error.isResourceSet()).thenReturn(true);
    when(primary.isFailed()).thenReturn(true);
    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_errorSet_notFailedPrimary_nonNullParentNotSet_returnsFalse() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(error.isResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isFalse();
  }

  @Test
  public void isAnyResourceSet_errorSet_failedPrimary_nonNullParentNotSet_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(primary.isFailed()).thenReturn(true);
    when(error.isResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_errorSet_nonNullParentSet_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.isAnyResourceSet()).thenReturn(true);
    when(error.isResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void isAnyResourceSet_primaryNotSet_errorNotSet_nonNullParentNotSet_returnsFalse() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);

    assertThat(coordinator.isAnyResourceSet()).isFalse();
  }

  @Test
  public void isAnyResourceSet_primaryNotSet_errorNotSet_nonNullParentSet_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);

    when(parent.isAnyResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isTrue();
  }

  @Test
  public void onRequestSuccess_nullParent_doesNotThrow() {
    coordinator.onRequestSuccess(primary);
  }

  @Test
  public void onRequestSuccess_nonNullParent_callsParent() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.onRequestSuccess(primary);
    verify(parent).onRequestSuccess(coordinator);
  }

  @Test
  public void onRequestFailed_primaryRequest_notRunningError_beingsError() {
    coordinator.onRequestFailed(primary);
    verify(error).begin();
  }

  @Test
  public void onRequestFailed_primaryRequest_runningError_doesNotBeginError() {
    when(error.isRunning()).thenReturn(true);
    coordinator.onRequestFailed(primary);

    verify(error, never()).begin();
  }

  @Test
  public void onRequestFailed_errorRequest_doesNotBeginError() {
    coordinator.onRequestFailed(error);
    verify(error, never()).begin();
  }

  @Test
  public void onRequestFailed_primaryRequest_notRunningError_nonNullParent_doesNotNotifyParent() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);

    coordinator.onRequestFailed(primary);
    verify(parent, never()).onRequestFailed(any(Request.class));
  }

  @Test
  public void onRequestFailed_errorRequest_nonNullParent_notifiesParent() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);

    coordinator.onRequestFailed(error);

    verify(parent).onRequestFailed(coordinator);
  }

  @Test
  public void onRequestFailed_primaryRequest_runningError_nonNullParent_doesNotNotifyParent() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(error.isRunning()).thenReturn(true);

    coordinator.onRequestFailed(primary);

    verify(parent, never()).onRequestFailed(any(Request.class));
  }

  @Test
  public void canNotifyCleared_primaryRequest_nullParent_returnsTrue() {
    assertThat(coordinator.canNotifyCleared(primary)).isTrue();
  }

  @Test
  public void canNotifyCleared_primaryRequest_parentCanNotNotify_returnsFalse() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);

    assertThat(coordinator.canNotifyCleared(primary)).isFalse();
  }

  @Test
  public void canNotifyCleared_primaryRequest_parentCanNotify_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyCleared(coordinator)).thenReturn(true);

    assertThat(coordinator.canNotifyCleared(primary)).isTrue();
  }

  @Test
  public void canNotifyCleared_primaryRequestFailed_parentCanNotify_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyCleared(coordinator)).thenReturn(true);
    when(primary.isFailed()).thenReturn(true);

    assertThat(coordinator.canNotifyCleared(primary)).isTrue();
  }

  @Test
  public void canNotifyCleared_primaryRequestFailed_parentCanNotNotify_returnsFalse() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(primary.isFailed()).thenReturn(false);

    assertThat(coordinator.canNotifyCleared(primary)).isFalse();
  }

  @Test
  public void canNotifyCleared_primaryRequestFailed_nullParent_returnsTrue() {
    when(primary.isFailed()).thenReturn(true);

    assertThat(coordinator.canNotifyCleared(primary)).isTrue();
  }

  @Test
  public void canNotifyCleared_errorRequest_nullParent_returnsFalse() {
    assertThat(coordinator.canNotifyCleared(error)).isFalse();
  }

  @Test
  public void canNotifyCleared_errorRequest_primaryFailed_nullParent_returnsTrue() {
    when(primary.isFailed()).thenReturn(true);
    assertThat(coordinator.canNotifyCleared(error)).isTrue();
  }

  @Test
  public void canNotifyCleared_errorRequest_primaryFailed_nonNullParentCanNotNotify_returnsFalse() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyCleared(coordinator)).thenReturn(false);
    when(primary.isFailed()).thenReturn(true);

    assertThat(coordinator.canNotifyCleared(error)).isFalse();
  }

  @Test
  public void canNotifyCleared_errorRequest_primaryFailed_nonNullParentCanNotify_returnsTrue() {
    coordinator = new ErrorRequestCoordinator(parent);
    coordinator.setRequests(primary, error);
    when(parent.canNotifyCleared(coordinator)).thenReturn(true);
    when(primary.isFailed()).thenReturn(true);

    assertThat(coordinator.canNotifyCleared(error)).isTrue();
  }
}
