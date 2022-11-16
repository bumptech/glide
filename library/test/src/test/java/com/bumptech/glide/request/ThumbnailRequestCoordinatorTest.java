package com.bumptech.glide.request;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(JUnit4.class)
public class ThumbnailRequestCoordinatorTest {
  @Mock private Request full;
  @Mock private Request thumb;
  @Mock private RequestCoordinator parent;
  private ThumbnailRequestCoordinator coordinator;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    coordinator = newCoordinator();
    coordinator.setRequests(full, thumb);
  }

  @Test
  public void testIsRunningIsFalseIfNeitherRequestIsRunning() {
    assertFalse(coordinator.isRunning());
  }

  @Test
  public void isRunning_withThumbAndFullRunning_isTrue() {
    coordinator.begin();
    assertTrue(coordinator.isRunning());
  }

  @Test
  public void isRunning_withFullRunning_isTrue() {
    coordinator.begin();
    coordinator.onRequestSuccess(thumb);
    assertTrue(coordinator.isRunning());
  }

  @Test
  public void isRunning_withThumbRunning_fullComplete_isFalse() {
    coordinator.begin();
    coordinator.onRequestSuccess(full);
    assertFalse(coordinator.isRunning());
  }

  @Test
  public void testStartsFullOnRunIfNotRunning() {
    coordinator.begin();

    verify(full).begin();
  }

  @Test
  public void testStartsThumbOnRunIfNotRunning() {
    coordinator.begin();

    verify(thumb).begin();
  }

  @Test
  public void testDoesNotStartFullOnRunIfRunning() {
    coordinator.begin();
    coordinator.begin();

    verify(full, times(1)).begin();
  }

  @Test
  public void testDoesNotStartThumbOnRunIfRunning() {
    coordinator.begin();
    coordinator.begin();

    verify(thumb, times(1)).begin();
  }

  @Test
  public void begin_whenFullIsComplete_startsFull() {
    coordinator.onRequestSuccess(full);

    coordinator.begin();

    verify(full).begin();
  }

  @Test
  public void begin_whenFullIsComplete_doesNotBeginThumb() {
    coordinator.onRequestSuccess(full);

    coordinator.begin();

    verify(thumb, never()).begin();
  }

  @Test
  public void testDoesNotStartFullIfClearedByThumb() {
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                coordinator.clear();

                return null;
              }
            })
        .when(thumb)
        .begin();

    coordinator.begin();

    verify(full, never()).begin();
  }

  @Test
  public void testCallsClearOnRequestsWhenCleared() {
    coordinator.clear();
    InOrder order = inOrder(thumb, full);
    order.verify(thumb).clear();
    order.verify(full).clear();
  }

  @Test
  public void pause_pausesThumbAndFullInOrder() {
    coordinator.begin();
    coordinator.pause();
    InOrder order = inOrder(thumb, full);
    order.verify(thumb).pause();
    order.verify(full).pause();
  }

  @Test
  public void testCanSetImageReturnsTrueForFullRequestIfCoordinatorIsNull() {
    coordinator = newCoordinator();
    coordinator.setRequests(full, thumb);
    assertTrue(coordinator.canSetImage(full));
  }

  @Test
  public void testCanSetImageReturnsTrueForFullRequestIfParentAllowsSetImage() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canSetImage(eq(coordinator))).thenReturn(true);
    assertTrue(coordinator.canSetImage(full));
  }

  @Test
  public void testCanSetImageReturnsFalseForFullRequestIfParentDoesNotAllowSetImage() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canSetImage(eq(coordinator))).thenReturn(false);
    assertFalse(coordinator.canSetImage(full));
  }

  @Test
  public void canSetImage_forThumb_withNullParent_fullNotComplete_returnsTrue() {
    assertTrue(coordinator.canSetImage(thumb));
  }

  @Test
  public void canSetImage_forThumb_withNullParent_fullComplete_returnsFalse() {
    coordinator.onRequestSuccess(full);
    assertFalse(coordinator.canSetImage(thumb));
  }

  @Test
  public void canSetImage_forThumb_whenDisallowedByParent_fullNotComplete_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canSetImage(eq(coordinator))).thenReturn(false);
    assertFalse(coordinator.canSetImage(thumb));
  }

  @Test
  public void canSetImage_forThumb_whenDisallowedByParent_fullComplete_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canSetImage(eq(coordinator))).thenReturn(false);
    coordinator.onRequestSuccess(full);
    assertFalse(coordinator.canSetImage(thumb));
  }

  @Test
  public void testCanNotifyStatusChangedIfFullAndNoRequestsAreComplete() {
    assertTrue(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void testCanNotNotifyStatusChangedIfThumb() {
    assertFalse(coordinator.canNotifyStatusChanged(thumb));
  }

  @Test
  public void canNotNotifyStatusChanged_forFull_whenFullComplete_isFalse() {
    when(full.isAnyResourceSet()).thenReturn(true);
    coordinator.onRequestSuccess(full);
    assertFalse(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void canNotNotifyStatusChanged_forFull_whenIfThumbComplete_isFalse() {
    when(thumb.isAnyResourceSet()).thenReturn(true);
    coordinator.onRequestSuccess(thumb);
    assertFalse(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void testCanNotNotifyStatusChangedIfParentHasResourceSet() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.isAnyResourceSet()).thenReturn(true);
    assertFalse(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void testCanNotifyStatusChangedIfParentAllowsNotify() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canNotifyStatusChanged(eq(coordinator))).thenReturn(true);
    assertTrue(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void testCanNotNotifyStatusChangedIfParentDoesNotAllowNotify() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canNotifyStatusChanged(eq(coordinator))).thenReturn(false);
    assertFalse(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void isAnyResourceSet_withIncompleteThumbAndFull_isFalse() {
    assertFalse(coordinator.isAnyResourceSet());
  }

  @Test
  public void isAnyResourceSet_withCompleteFull_isTrue() {
    when(full.isAnyResourceSet()).thenReturn(true);
    coordinator.onRequestSuccess(full);
    assertTrue(coordinator.isAnyResourceSet());
  }

  @Test
  public void isAnyResourceSet_withCompleteThumb_isTrue() {
    when(thumb.isAnyResourceSet()).thenReturn(true);
    coordinator.onRequestSuccess(thumb);
    assertTrue(coordinator.isAnyResourceSet());
  }

  @Test
  public void isAnyResourceSet_withParentResourceSet_isFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);

    when(parent.isAnyResourceSet()).thenReturn(true);

    assertThat(coordinator.isAnyResourceSet()).isFalse();
  }

  @Test
  public void testIsNotCompleteIfNeitherRequestIsComplete() {
    assertFalse(coordinator.isComplete());
  }

  @Test
  public void isComplete_withFullComplete_isTrue() {
    coordinator.onRequestSuccess(full);
    assertTrue(coordinator.isComplete());
  }

  @Test
  public void isComplete_withOnlyThumbComplete_returnsFalse() {
    coordinator.onRequestSuccess(thumb);
    assertThat(coordinator.isComplete()).isFalse();
  }

  @Test
  public void testClearsThumbRequestOnFullRequestComplete_withNullParent() {
    coordinator.onRequestSuccess(full);
    verify(thumb).clear();
  }

  @Test
  public void testNotifiesParentOnFullRequestComplete_withNonNullParent() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    coordinator.onRequestSuccess(full);
    verify(parent).onRequestSuccess(eq(coordinator));
  }

  @Test
  public void testClearsThumbRequestOnFullRequestComplete_withNonNullParent() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    coordinator.onRequestSuccess(full);
    verify(thumb).clear();
  }

  @Test
  public void testDoesNotClearThumbOnThumbRequestComplete() {
    coordinator.onRequestSuccess(thumb);
    verify(thumb, never()).clear();
  }

  @Test
  public void testDoesNotClearThumbOnFullComplete_whenThumbIsComplete() {
    coordinator.onRequestSuccess(thumb);
    coordinator.onRequestSuccess(full);
    verify(thumb, never()).clear();
  }

  @Test
  public void testDoesNotNotifyParentOnThumbRequestComplete() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    coordinator.onRequestSuccess(thumb);

    verify(parent, never()).onRequestSuccess(any(Request.class));
  }

  @Test
  public void canNotifyCleared_withThumbRequest_returnsFalse() {
    assertThat(coordinator.canNotifyCleared(thumb)).isFalse();
  }

  @Test
  public void canNotifyCleared_withFullRequest_andNullParent_returnsTrue() {
    assertThat(coordinator.canNotifyCleared(full)).isTrue();
  }

  @Test
  public void canNotifyCleared_withFullRequest_nonNullParent_parentCanClear_returnsTrue() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canNotifyCleared(coordinator)).thenReturn(true);
    assertThat(coordinator.canNotifyCleared(full)).isTrue();
  }

  @Test
  public void canNotifyCleared_withFullRequest_nonNullParent_parentCanNotClear_returnsFalse() {
    coordinator = newCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canNotifyCleared(coordinator)).thenReturn(false);
    assertThat(coordinator.canNotifyCleared(full)).isFalse();
  }

  @Test
  public void canNotifyCleared_withFullRequest_afterPause_returnsFalse() {
    coordinator.pause();
    assertThat(coordinator.canNotifyCleared(full)).isFalse();
  }

  @Test
  public void canNotifyCleared_withFullRequest_afterPauseAndResume_returnsTrue() {
    coordinator.pause();
    coordinator.begin();
    assertThat(coordinator.canNotifyCleared(full)).isTrue();
  }

  @Test
  public void canNotifyCleared_withFullRequest_afterPauseAndClear_returnsTrue() {
    coordinator.pause();
    coordinator.clear();
    assertThat(coordinator.canNotifyCleared(full)).isTrue();
  }

  @Test
  public void testIsEquivalentTo() {
    ThumbnailRequestCoordinator first = newCoordinator();
    when(full.isEquivalentTo(full)).thenReturn(true);
    when(thumb.isEquivalentTo(thumb)).thenReturn(true);
    first.setRequests(full, thumb);
    assertTrue(first.isEquivalentTo(first));

    ThumbnailRequestCoordinator second = newCoordinator();
    second.setRequests(full, full);
    assertTrue(second.isEquivalentTo(second));
    assertFalse(second.isEquivalentTo(first));
    assertFalse(first.isEquivalentTo(second));

    ThumbnailRequestCoordinator third = newCoordinator();
    third.setRequests(thumb, thumb);
    assertTrue(third.isEquivalentTo(third));
    assertFalse(third.isEquivalentTo(first));
    assertFalse(first.isEquivalentTo(third));
  }

  private static ThumbnailRequestCoordinator newCoordinator() {
    return newCoordinator(/* parent= */ null);
  }

  private static ThumbnailRequestCoordinator newCoordinator(RequestCoordinator parent) {
    return new ThumbnailRequestCoordinator(/* requestLock= */ new Object(), parent);
  }
}
