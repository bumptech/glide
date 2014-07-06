package com.bumptech.glide.request;

import android.os.Handler;

import com.bumptech.glide.request.target.Target;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class RequestFutureTargetTest {
    private int width;
    private int height;
    private RequestFutureTarget<Object, Object> future;
    private Request request;
    private Handler handler;

    @Before
    public void setUp() {
        width = 100;
        height = 100;
        handler = mock(Handler.class);
        future = new RequestFutureTarget<Object, Object>(handler, width, height, false);
        request = mock(Request.class);
        future.setRequest(request);
    }

    @Test
    public void testAlwaysReturnsTrueFromOnResourceReady() {
        assertTrue(future.onResourceReady(null, null, null, false, false));
    }

    @Test
    public void testAlwaysReturnsTrueFromOnException() {
        assertTrue(future.onException(null, null, null, false));
    }

    @Test
    public void testCallsSizeReadyCallbackOnGetSize() {
        Target.SizeReadyCallback cb = mock(Target.SizeReadyCallback.class);
        future.getSize(cb);
        verify(cb).onSizeReady(eq(width), eq(height));
    }

    @Test
    public void testReturnsFalseForDoneBeforeDone() {
        assertFalse(future.isDone());
    }

    @Test
    public void testReturnsTrueFromIsDoneIfDone() {
        future.onResourceReady(new Object(), null, null, false, false);
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
    public void testClearsRequestOnMainThreadIfNotYetDoneOnCancel() {
        future.cancel(true);

        verify(handler).post(eq(future));
    }

    @Test
    public void testClearsOnMainThreadWhenClearCalled() {
        future.clear();

        verify(handler).post(eq(future));
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
        future.onResourceReady(new Object(), null, null, false, false);
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
        future.onResourceReady(new Object(), null, null, false, false);
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
        future.onResourceReady(new Object(), null, null, false, false);
        assertFalse(future.cancel(true));
    }

    @Test
    public void testReturnsResourceOnGetIfAlreadyDone() throws ExecutionException, InterruptedException {
        Object expected = new Object();
        future.onResourceReady(expected, null, null, false, false);

        assertEquals(expected, future.get());
    }

    @Test
    public void testReturnsResourceOnGetWithTimeoutIfAlreadyDone() throws InterruptedException, ExecutionException,
            TimeoutException {
        Object expected = new Object();
        future.onResourceReady(expected, null, null, false, false);

        assertEquals(expected, future.get(100, TimeUnit.MILLISECONDS));
    }

    @Test(expected = CancellationException.class)
    public void testThrowsCancellationExceptionIfCancelledBeforeGet() throws ExecutionException, InterruptedException {
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
        future.onException(new RuntimeException(), null, null, false);
        future.get();
    }

    @Test(expected = ExecutionException.class)
    public void testThrowsExecutionExceptionOnGetIfExceptionWithNullValueBeforeGet()
            throws ExecutionException, InterruptedException, TimeoutException {
        future.onException(null, null, null, false);
        future.get(100, TimeUnit.MILLISECONDS);
    }

    @Test(expected = ExecutionException.class)
    public void testThrowsExecutionExceptionOnGetIfExceptionBeforeGetWithTimeout()
            throws ExecutionException, InterruptedException, TimeoutException {
        future.onException(new RuntimeException(), null, null, false);
        future.get(100, TimeUnit.MILLISECONDS);
    }

    @Test(expected = TimeoutException.class)
    public void testThrowsTimeoutExceptionOnGetIfFailedToReceiveResourceInTime()
            throws InterruptedException, ExecutionException, TimeoutException {
        future.get(1, TimeUnit.MILLISECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsExceptionIfGetCalledOnMainThread() throws ExecutionException, InterruptedException {
        future = new RequestFutureTarget<Object, Object>(handler, width, height, true);
        future.get();
    }
}