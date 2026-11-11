package com.bumptech.glide.request;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.target.SizeReadyCallback;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class RequestFutureTargetTest {
  private int width;
  private int height;
  private RequestFutureTarget<Object> future;
  private Request request;
  private RequestFutureTarget.Waiter waiter;

  @Before
  public void setUp() {
    width = 100;
    height = 100;
    waiter = mock(RequestFutureTarget.Waiter.class);
    future = new RequestFutureTarget<>(width, height, false, waiter);
    request = mock(Request.class);
    future.setRequest(request);
  }

  @Test
  public void callsSizeReadyCallbackOnGetSize() {
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    future.getSize(cb);
    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void returnsFalseForDoneBeforeDone() {
    assertFalse(future.isDone());
  }

  @Test
  public void returnsTrueFromIsDoneIfDone() {
    future.onResourceReady(
        /* resource= */ new Object(),
        /* model= */ null,
        /* target= */ future,
        DataSource.DATA_DISK_CACHE,
        true /*isFirstResource*/);
    assertTrue(future.isDone());
  }

  @Test
  public void returnsFalseForIsCancelledBeforeCancelled() {
    assertFalse(future.isCancelled());
  }

  @Test
  public void returnsTrueFromCancelIfNotYetDone() {
    assertTrue(future.cancel(false));
  }

  @Test
  public void cancel_withMayInterruptIfRunningTrueAndNotFinishedRequest_clearsFuture() {
    future.cancel(true);

    verify(request).clear();
  }

  @Test
  public void cancel_withInterruptFalseAndNotFinishedRequest_doesNotClearFuture() {
    future.cancel(false);

    verify(request, never()).clear();
  }

  @Test
  public void doesNotRepeatedlyClearRequestIfCancelledRepeatedly() {
    future.cancel(true);
    future.cancel(true);

    verify(request, times(1)).clear();
  }

  @Test
  public void doesNotClearRequestIfCancelledAfterDone() {
    future.onResourceReady(
        /* resource= */ new Object(),
        /* model= */ null,
        /* target= */ future,
        DataSource.DATA_DISK_CACHE,
        true /*isFirstResource*/);
    future.cancel(true);

    verify(request, never()).clear();
  }

  @Test
  public void returnsTrueFromDoneIfCancelled() {
    future.cancel(true);
    assertTrue(future.isDone());
  }

  @Test
  public void returnsFalseFromIsCancelledIfCancelledAfterDone() {
    future.onResourceReady(
        /* resource= */ new Object(),
        /* model= */ null,
        /* target= */ future,
        DataSource.DATA_DISK_CACHE,
        true /*isFirstResource*/);
    future.cancel(true);

    assertFalse(future.isCancelled());
  }

  @Test
  public void returnsTrueFromCancelIfCancelled() {
    future.cancel(true);
    assertTrue(future.isCancelled());
  }

  @Test
  public void returnsFalseFromCancelIfDone() {
    future.onResourceReady(
        /* resource= */ new Object(),
        /* model= */ null,
        /* target= */ future,
        DataSource.DATA_DISK_CACHE,
        true /*isFirstResource*/);
    assertFalse(future.cancel(true));
  }

  @Test
  public void returnsResourceOnGetIfAlreadyDone() throws ExecutionException, InterruptedException {
    Object expected = new Object();
    future.onResourceReady(
        /* resource= */ expected,
        /* model= */ null,
        /* target= */ future,
        DataSource.DATA_DISK_CACHE,
        true /*isFirstResource*/);

    assertEquals(expected, future.get());
  }

  @Test
  public void returnsResourceOnGetWithTimeoutIfAlreadyDone()
      throws InterruptedException, ExecutionException, TimeoutException {
    Object expected = new Object();
    future.onResourceReady(
        /* resource= */ expected,
        /* model= */ null,
        /* target= */ future,
        DataSource.DATA_DISK_CACHE,
        true /*isFirstResource*/);

    assertEquals(expected, future.get(1, TimeUnit.MILLISECONDS));
  }

  @Test(expected = CancellationException.class)
  public void throwsCancellationExceptionIfCancelledBeforeGet()
      throws ExecutionException, InterruptedException {
    future.cancel(true);
    future.get();
  }

  @Test(expected = CancellationException.class)
  public void throwsCancellationExceptionIfCancelledBeforeGetWithTimeout()
      throws InterruptedException, ExecutionException, TimeoutException {
    future.cancel(true);
    future.get(100, TimeUnit.MILLISECONDS);
  }

  @Test(expected = ExecutionException.class)
  public void throwsExecutionExceptionOnGetIfExceptionBeforeGet()
      throws ExecutionException, InterruptedException {
    future.onLoadFailed(/* e= */ null, /* model= */ null, future, /* isFirstResource= */ true);
    future.get();
  }

  @Test(expected = ExecutionException.class)
  public void throwsExecutionExceptionOnGetIfExceptionWithNullValueBeforeGet()
      throws ExecutionException, InterruptedException, TimeoutException {
    future.onLoadFailed(/* e= */ null, /* model= */ null, future, /* isFirstResource= */ true);
    future.get(100, TimeUnit.MILLISECONDS);
  }

  @Test(expected = ExecutionException.class)
  public void throwsExecutionExceptionOnGetIfExceptionBeforeGetWithTimeout()
      throws ExecutionException, InterruptedException, TimeoutException {
    future.onLoadFailed(/* e= */ null, /* model= */ null, future, /* isFirstResource= */ true);
    future.get(100, TimeUnit.MILLISECONDS);
  }

  @Test(expected = TimeoutException.class)
  public void throwsTimeoutExceptionOnGetIfFailedToReceiveResourceInTime()
      throws InterruptedException, ExecutionException, TimeoutException {
    future.get(1, TimeUnit.MILLISECONDS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwsExceptionIfGetCalledOnMainThread()
      throws ExecutionException, InterruptedException {
    future = new RequestFutureTarget<>(width, height, true, waiter);
    future.get();
  }

  @Test
  public void getSucceedsOnMainThreadIfDone() throws ExecutionException, InterruptedException {
    future = new RequestFutureTarget<>(width, height, true, waiter);
    future.onResourceReady(
        /* resource= */ new Object(),
        /* model= */ null,
        /* target= */ future,
        DataSource.DATA_DISK_CACHE,
        true /*isFirstResource*/);
    future.get();
  }

  @Test(expected = InterruptedException.class)
  public void throwsInterruptedExceptionIfThreadInterruptedWhenDoneWaiting()
      throws InterruptedException, ExecutionException {
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                Thread.currentThread().interrupt();
                return null;
              }
            })
        .when(waiter)
        .waitForTimeout(eq(future), anyLong());

    future.get();
  }

  @Test(expected = ExecutionException.class)
  public void throwsExecutionExceptionIfLoadFailsWhileWaiting()
      throws ExecutionException, InterruptedException {
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                future.onLoadFailed(
                    /* e= */ null, /* model= */ null, future, /* isFirstResource= */ true);
                return null;
              }
            })
        .when(waiter)
        .waitForTimeout(eq(future), anyLong());
    future.get();
  }

  @Test(expected = CancellationException.class)
  public void throwsCancellationExceptionIfCancelledWhileWaiting()
      throws ExecutionException, InterruptedException {
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                future.cancel(false);
                return null;
              }
            })
        .when(waiter)
        .waitForTimeout(eq(future), anyLong());
    future.get();
  }

  @Test(expected = TimeoutException.class)
  public void throwsTimeoutExceptionIfFinishesWaitingWithTimeoutAndDoesNotReceiveResult()
      throws ExecutionException, InterruptedException, TimeoutException {
    future.get(1, TimeUnit.MILLISECONDS);
  }

  @Test(expected = AssertionError.class)
  public void throwsAssertionErrorIfFinishesWaitingWithoutTimeoutAndDoesNotReceiveResult()
      throws ExecutionException, InterruptedException {
    future.get();
  }

  @Test
  public void notifiesAllWhenLoadFails() {
    future.onLoadFailed(/* e= */ null, /* model= */ null, future, /* isFirstResource= */ true);
    verify(waiter).notifyAll(eq(future));
  }

  @Test
  public void notifiesAllWhenResourceReady() {
    future.onResourceReady(
        /* resource= */ new Object(),
        /* model= */ null,
        /* target= */ future,
        DataSource.DATA_DISK_CACHE,
        true /*isFirstResource*/);
    verify(waiter).notifyAll(eq(future));
  }

  @Test
  public void notifiesAllOnCancelIfNotCancelled() {
    future.cancel(false);
    verify(waiter).notifyAll(eq(future));
  }

  @Test
  public void doesNotNotifyAllOnSecondCancel() {
    future.cancel(true);
    verify(waiter).notifyAll(eq(future));
    future.cancel(true);
    verify(waiter, times(1)).notifyAll(eq(future));
  }

  @Test
  public void returnsResourceIfReceivedWhileWaiting()
      throws ExecutionException, InterruptedException {
    final Object expected = new Object();
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                future.onResourceReady(
                    /* resource= */ expected,
                    /* model= */ null,
                    /* target= */ future,
                    DataSource.DATA_DISK_CACHE,
                    true /*isFirstResource*/);
                return null;
              }
            })
        .when(waiter)
        .waitForTimeout(eq(future), anyLong());
    assertEquals(expected, future.get());
  }

  @Test
  public void waitsForeverIfNoTimeoutSet() throws InterruptedException {
    try {
      future.get();
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } catch (AssertionError e) {
      // Expected.
    }
    verify(waiter).waitForTimeout(eq(future), eq(0L));
  }

  @Test
  public void waitsForGivenTimeoutMillisIfTimeoutSet() throws InterruptedException {
    long timeout = 2;
    try {
      future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      // Expected.
    }

    verify(waiter, atLeastOnce()).waitForTimeout(eq(future), eq(timeout));
  }

  @Test
  public void convertsOtherTimeUnitsToMillisForWaiter() throws InterruptedException {
    long timeoutMicros = 1000;
    try {
      future.get(timeoutMicros, TimeUnit.MICROSECONDS);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      // Expected.
    }

    verify(waiter, atLeastOnce())
        .waitForTimeout(eq(future), eq(TimeUnit.MICROSECONDS.toMillis(timeoutMicros)));
  }

  @Test
  public void doesNotWaitIfGivenTimeOutEqualToZero() throws InterruptedException {
    try {
      future.get(0, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      // Expected.
    }

    verify(waiter, never()).waitForTimeout(eq(future), anyLong());
  }
}
