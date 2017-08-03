package com.bumptech.glide.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.os.Handler;
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
@Config(manifest = Config.NONE, sdk = 18)
public class RequestFutureTargetTest {
  private int width;
  private int height;
  private RequestFutureTarget<Object> future;
  private Request request;
  private Handler handler;
  private RequestFutureTarget.Waiter waiter;

  @Before
  public void setUp() {
    width = 100;
    height = 100;
    handler = mock(Handler.class);
    waiter = mock(RequestFutureTarget.Waiter.class);
    future = new RequestFutureTarget<>(handler, width, height, false, waiter);
    request = mock(Request.class);
    future.setRequest(request);
  }

  @Test
  public void testCallsSizeReadyCallbackOnGetSize() {
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    future.getSize(cb);
    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testReturnsFalseForDoneBeforeDone() {
    assertFalse(future.isDone());
  }

  @Test
  public void testReturnsTrueFromIsDoneIfDone() {
    future.onResourceReady(new Object(), null);
    assertTrue(future.isDone());
  }

  @Test
  public void testReturnsFalseForIsCancelledBeforeCancelled() {
    assertFalse(future.isCancelled());
  }

  @Test
  public void testReturnsTrueFromCancelIfNotYetDone() {
    assertTrue(future.cancel(false));
  }

  @Test
  public void cancel_withMayInterruptIfRunningTrueAndNotFinishedRequest_clearsFutureOnMainThread() {
    future.cancel(true);

    verify(handler).post(eq(future));
  }

  @Test
  public void cancel_withInterruptFalseAndNotFinishedRequest_doesNotclearFutureOnMainThread() {
    future.cancel(false);

    verify(handler, never()).post(eq(future));
  }

  @Test
  public void testDoesNotRepeatedlyClearRequestOnMainThreadIfCancelledRepeatedly() {
    future.cancel(true);
    future.cancel(true);

    verify(handler, times(1)).post(any(Runnable.class));
  }

  @Test
  public void testClearsRequestOnRun() {
    future.run();

    verify(request).clear();
  }

  @Test
  public void testDoesNotClearRequestIfCancelledAfterDone() {
    future.onResourceReady(new Object(), null);
    future.cancel(true);

    verify(request, never()).clear();
  }

  @Test
  public void testReturnsTrueFromDoneIfCancelled() {
    future.cancel(true);
    assertTrue(future.isDone());
  }

  @Test
  public void testReturnsFalseFromIsCancelledIfCancelledAfterDone() {
    future.onResourceReady(new Object(), null);
    future.cancel(true);

    assertFalse(future.isCancelled());
  }

  @Test
  public void testReturnsTrueFromCancelIfCancelled() {
    future.cancel(true);
    assertTrue(future.isCancelled());
  }

  @Test
  public void testReturnsFalseFromCancelIfDone() {
    future.onResourceReady(new Object(), null);
    assertFalse(future.cancel(true));
  }

  @Test
  public void testReturnsResourceOnGetIfAlreadyDone()
      throws ExecutionException, InterruptedException {
    Object expected = new Object();
    future.onResourceReady(expected, null);

    assertEquals(expected, future.get());
  }

  @Test
  public void testReturnsResourceOnGetWithTimeoutIfAlreadyDone()
      throws InterruptedException, ExecutionException, TimeoutException {
    Object expected = new Object();
    future.onResourceReady(expected, null);

    assertEquals(expected, future.get(100, TimeUnit.MILLISECONDS));
  }

  @Test(expected = CancellationException.class)
  public void testThrowsCancellationExceptionIfCancelledBeforeGet()
      throws ExecutionException, InterruptedException {
    future.cancel(true);
    future.get();
  }

  @Test(expected = CancellationException.class)
  public void testThrowsCancellationExceptionIfCancelledBeforeGetWithTimeout()
      throws InterruptedException, ExecutionException, TimeoutException {
    future.cancel(true);
    future.get(100, TimeUnit.MILLISECONDS);
  }

  @Test(expected = ExecutionException.class)
  public void testThrowsExecutionExceptionOnGetIfExceptionBeforeGet()
      throws ExecutionException, InterruptedException {
    future.onLoadFailed(null);
    future.get();
  }

  @Test(expected = ExecutionException.class)
  public void testThrowsExecutionExceptionOnGetIfExceptionWithNullValueBeforeGet()
      throws ExecutionException, InterruptedException, TimeoutException {
    future.onLoadFailed(null);
    future.get(100, TimeUnit.MILLISECONDS);
  }

  @Test(expected = ExecutionException.class)
  public void testThrowsExecutionExceptionOnGetIfExceptionBeforeGetWithTimeout()
      throws ExecutionException, InterruptedException, TimeoutException {
    future.onLoadFailed(null);
    future.get(100, TimeUnit.MILLISECONDS);
  }

  @Test(expected = TimeoutException.class)
  public void testThrowsTimeoutExceptionOnGetIfFailedToReceiveResourceInTime()
      throws InterruptedException, ExecutionException, TimeoutException {
    future.get(1, TimeUnit.MILLISECONDS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsExceptionIfGetCalledOnMainThread()
      throws ExecutionException, InterruptedException {
    future = new RequestFutureTarget<>(handler, width, height, true, waiter);
    future.get();
  }

  @Test
  public void testGetSucceedsOnMainThreadIfDone()
      throws ExecutionException, InterruptedException {
    future = new RequestFutureTarget<>(handler, width, height, true, waiter);
    future.onResourceReady(new Object(), null);
    future.get();
  }

  @Test(expected = InterruptedException.class)
  public void testThrowsInterruptedExceptionIfThreadInterruptedWhenDoneWaiting()
      throws InterruptedException, ExecutionException {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        Thread.currentThread().interrupt();
        return null;
      }
    }).when(waiter).waitForTimeout(eq(future), anyLong());

    future.get();
  }

  @Test(expected = ExecutionException.class)
  public void testThrowsExecutionExceptionIfLoadFailsWhileWaiting()
      throws ExecutionException, InterruptedException {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        future.onLoadFailed(null);
        return null;
      }
    }).when(waiter).waitForTimeout(eq(future), anyLong());
    future.get();
  }

  @Test(expected = CancellationException.class)
  public void testThrowsCancellationExceptionIfCancelledWhileWaiting()
      throws ExecutionException, InterruptedException {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        future.cancel(false);
        return null;
      }
    }).when(waiter).waitForTimeout(eq(future), anyLong());
    future.get();
  }

  @Test(expected = TimeoutException.class)
  public void testThrowsTimeoutExceptionIfFinishesWaitingWithTimeoutAndDoesNotReceiveResult()
      throws ExecutionException, InterruptedException, TimeoutException {
    future.get(1, TimeUnit.MILLISECONDS);
  }

  @Test(expected = AssertionError.class)
  public void testThrowsAssertionErrorIfFinishesWaitingWithoutTimeoutAndDoesNotReceiveResult()
      throws ExecutionException, InterruptedException {
    future.get();
  }

  @Test
  public void testNotifiesAllWhenLoadFails() {
    future.onLoadFailed(null);
    verify(waiter).notifyAll(eq(future));
  }

  @Test
  public void testNotifiesAllWhenResourceReady() {
    future.onResourceReady(null, null);
    verify(waiter).notifyAll(eq(future));
  }

  @Test
  public void testNotifiesAllOnCancelIfNotCancelled() {
    future.cancel(false);
    verify(waiter).notifyAll(eq(future));
  }

  @Test
  public void testDoesNotNotifyAllOnSecondCancel() {
    future.cancel(true);
    verify(waiter).notifyAll(eq(future));
    future.cancel(true);
    verify(waiter, times(1)).notifyAll(eq(future));
  }

  @Test
  public void testReturnsResourceIfReceivedWhileWaiting()
      throws ExecutionException, InterruptedException {
    final Object expected = new Object();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        future.onResourceReady(expected, null);
        return null;
      }
    }).when(waiter).waitForTimeout(eq(future), anyLong());
    assertEquals(expected, future.get());
  }

  @Test
  public void testWaitsForeverIfNoTimeoutSet() throws InterruptedException {
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
  public void testWaitsForGivenTimeoutMillisIfTimeoutSet() throws InterruptedException {
    long timeout = 1234;
    try {
      future.get(1234, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      // Expected.
    }

    verify(waiter).waitForTimeout(eq(future), eq(timeout));
  }

  @Test
  public void testConvertsOtherTimeUnitsToMillisForWaiter() throws InterruptedException {
    long timeoutSeconds = 10;
    try {
      future.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      // Expected.
    }

    verify(waiter).waitForTimeout(eq(future), eq(TimeUnit.SECONDS.toMillis(timeoutSeconds)));
  }

  @Test
  public void testDoesNotWaitIfGivenTimeOutEqualToZero() throws InterruptedException {
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
