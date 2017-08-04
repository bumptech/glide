package com.bumptech.glide.request;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(JUnit4.class)
public class ThumbnailRequestCoordinatorTest {
  private Request full;
  private Request thumb;
  private ThumbnailRequestCoordinator coordinator;
  private RequestCoordinator parent;

  @Before
  public void setUp() {
    full = mock(Request.class);
    thumb = mock(Request.class);
    parent = mock(RequestCoordinator.class);
    coordinator = new ThumbnailRequestCoordinator();
    coordinator.setRequests(full, thumb);
  }

  @Test
  public void testIsRunningIsFalseIfNeitherRequestIsRunning() {
    assertFalse(coordinator.isRunning());
  }

  @Test
  public void testIsRunningIsTrueIfFullIsRunning() {
    when(full.isRunning()).thenReturn(true);
    assertTrue(coordinator.isRunning());
  }

  @Test
  public void testIsNotRunningIfFullIsNotRunningButThumbIs() {
    when(full.isRunning()).thenReturn(false);
    when(thumb.isRunning()).thenReturn(true);
    assertFalse(coordinator.isRunning());
  }

  @Test
  public void testStartsFullOnRunIfNotRunning() {
    when(full.isRunning()).thenReturn(false);
    coordinator.begin();

    verify(full).begin();
  }

  @Test
  public void testStartsThumbOnRunIfNotRunning() {
    when(thumb.isRunning()).thenReturn(false);
    coordinator.begin();

    verify(thumb).begin();
  }

  @Test
  public void testDoesNotStartFullOnRunIfRunning() {
    when(full.isRunning()).thenReturn(true);
    coordinator.begin();

    verify(full, never()).begin();
  }

  @Test
  public void testDoesNotStartThumbOnRunIfRunning() {
    when(thumb.isRunning()).thenReturn(true);
    coordinator.begin();

    verify(thumb, never()).begin();
  }

  @Test
  public void testDoesNotStartFullIfClearedByThumb() {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        coordinator.clear();

        return null;
      }
    }).when(thumb).begin();

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
  public void testRecyclesRequestsWhenRecycled() {
    coordinator.recycle();
    verify(thumb).recycle();
    verify(full).recycle();
  }

  @Test
  public void testIsPausedWhenFullIsPaused() {
    when(full.isPaused()).thenReturn(true);
    assertTrue(coordinator.isPaused());
  }

  @Test
  public void testPausesBothRequestsWhenPaused() {
    coordinator.pause();
    verify(full).pause();
    verify(thumb).pause();
  }

  @Test
  public void testCanSetImageReturnsTrueForFullRequestIfCoordinatorIsNull() {
    coordinator = new ThumbnailRequestCoordinator();
    coordinator.setRequests(full, thumb);
    assertTrue(coordinator.canSetImage(full));
  }

  @Test
  public void testCanSetImageReturnsTrueForFullRequestIfParentAllowsSetImage() {
    coordinator = new ThumbnailRequestCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canSetImage(eq(coordinator))).thenReturn(true);
    assertTrue(coordinator.canSetImage(full));
  }

  @Test
  public void testCanSetImageReturnsFalseForFullRequestIfParentDoesNotAllowSetImage() {
    coordinator = new ThumbnailRequestCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canSetImage(eq(coordinator))).thenReturn(false);
    assertFalse(coordinator.canSetImage(full));
  }

  @Test
  public void
  testCanSetImageReturnsTrueForThumbRequestIfParentIsNullAndFullDoesNotHaveResourceSet() {
    when(full.isResourceSet()).thenReturn(false);
    assertTrue(coordinator.canSetImage(thumb));
  }

  @Test
  public void testCanSetImageReturnsFalseForThumbRequestIfParentIsNullAndFullHasResourceSet() {
    when(full.isResourceSet()).thenReturn(true);
    assertFalse(coordinator.canSetImage(thumb));
  }

  @Test
  public void testCanNotSetImageForThumbIfNotAllowedByParentAndFullDoesNotHaveResourceSet() {
    coordinator = new ThumbnailRequestCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canSetImage(eq(coordinator))).thenReturn(false);
    when(full.isResourceSet()).thenReturn(false);
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
  public void testCanNotNotifyStatusChangedIfFullHasResourceSet() {
    when(full.isResourceSet()).thenReturn(true);
    assertFalse(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void testCanNotNotifyStatusChangedIfThumbHasResourceSet() {
    when(thumb.isResourceSet()).thenReturn(true);
    assertFalse(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void testCanNotNotifyStatusChangedIfParentHasResourceSet() {
    coordinator = new ThumbnailRequestCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.isAnyResourceSet()).thenReturn(true);
    assertFalse(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void testCanNotifyStatusChangedIfParentAllowsNotify() {
    coordinator = new ThumbnailRequestCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canNotifyStatusChanged(eq(coordinator))).thenReturn(true);
    assertTrue(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void testCanNotNotifyStatusChangedIfParentDoesNotAllowNotify() {
    coordinator = new ThumbnailRequestCoordinator(parent);
    coordinator.setRequests(full, thumb);
    when(parent.canNotifyStatusChanged(eq(coordinator))).thenReturn(false);
    assertFalse(coordinator.canNotifyStatusChanged(full));
  }

  @Test
  public void testIsAnyResourceSetIsFalseIfNeitherRequestHasResourceSet() {
    when(full.isResourceSet()).thenReturn(false);
    when(thumb.isResourceSet()).thenReturn(false);
    assertFalse(coordinator.isAnyResourceSet());
  }

  @Test
  public void testIsAnyResourceSetIsTrueIfFullHasResourceSet() {
    when(full.isResourceSet()).thenReturn(true);
    when(thumb.isResourceSet()).thenReturn(false);
    assertTrue(coordinator.isAnyResourceSet());
  }

  @Test
  public void testIsAnyResourceSetIsTrueIfThumbHasResourceSet() {
    when(full.isResourceSet()).thenReturn(false);
    when(thumb.isResourceSet()).thenReturn(true);
    assertTrue(coordinator.isAnyResourceSet());
  }

  @Test
  public void testIsAnyResourceSetIsTrueIfParentIsNonNullAndParentHasResourceSet() {
    coordinator = new ThumbnailRequestCoordinator(parent);
    coordinator.setRequests(full, thumb);

    when(parent.isAnyResourceSet()).thenReturn(true);
    when(full.isResourceSet()).thenReturn(false);
    when(thumb.isResourceSet()).thenReturn(false);

    assertTrue(coordinator.isAnyResourceSet());
  }

  @Test
  public void testIsNotCompleteIfNeitherRequestIsComplete() {
    assertFalse(coordinator.isComplete());
  }

  @Test
  public void testIsCompleteIfFullIsComplete() {
    when(full.isComplete()).thenReturn(true);
    assertTrue(coordinator.isComplete());
  }

  @Test
  public void testIsCompleteIfThumbIsComplete() {
    when(thumb.isComplete()).thenReturn(true);
    assertTrue(coordinator.isComplete());
  }

  @Test
  public void testIsResourceSetIsFalseIfNeitherRequestHasResourceSet() {
    assertFalse(coordinator.isResourceSet());
  }

  @Test
  public void testIsResourceSetIsTrueIfFullRequestHasResourceSet() {
    when(full.isResourceSet()).thenReturn(true);
    assertTrue(coordinator.isResourceSet());
  }

  @Test
  public void testIsResourceSetIsTrueIfThumbRequestHasResourceSet() {
    when(thumb.isResourceSet()).thenReturn(true);
    assertTrue(coordinator.isResourceSet());
  }

  @Test
  public void testClearsThumbRequestOnFullRequestComplete_withNullParent() {
    coordinator.onRequestSuccess(full);
    verify(thumb).clear();
  }

  @Test
  public void testNotifiesParentOnFullRequestComplete_withNonNullParent() {
    coordinator = new ThumbnailRequestCoordinator(parent);
    coordinator.setRequests(full, thumb);
    coordinator.onRequestSuccess(full);
    verify(parent).onRequestSuccess(eq(coordinator));
  }

  @Test
  public void testClearsThumbRequestOnFullRequestComplete_withNonNullParent() {
    coordinator = new ThumbnailRequestCoordinator(parent);
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
      when(thumb.isComplete()).thenReturn(true);
      coordinator.onRequestSuccess(full);
      verify(thumb, never()).clear();
  }

  @Test
  public void testDoesNotNotifyParentOnThumbRequestComplete() {
    coordinator = new ThumbnailRequestCoordinator(parent);
    coordinator.setRequests(full, thumb);
    coordinator.onRequestSuccess(thumb);

    verify(parent, never()).onRequestSuccess(any(Request.class));
  }

  @Test
  public void testIsEquivalentTo() {
    ThumbnailRequestCoordinator first = new ThumbnailRequestCoordinator();
    when(full.isEquivalentTo(full)).thenReturn(true);
    when(thumb.isEquivalentTo(thumb)).thenReturn(true);
    first.setRequests(full, thumb);
    assertTrue(first.isEquivalentTo(first));

    ThumbnailRequestCoordinator second = new ThumbnailRequestCoordinator();
    second.setRequests(full, full);
    assertTrue(second.isEquivalentTo(second));
    assertFalse(second.isEquivalentTo(first));
    assertFalse(first.isEquivalentTo(second));

    ThumbnailRequestCoordinator third = new ThumbnailRequestCoordinator();
    third.setRequests(thumb, thumb);
    assertTrue(third.isEquivalentTo(third));
    assertFalse(third.isEquivalentTo(first));
    assertFalse(first.isEquivalentTo(third));
  }
}
