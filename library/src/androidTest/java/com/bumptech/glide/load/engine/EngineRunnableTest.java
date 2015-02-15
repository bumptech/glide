package com.bumptech.glide.load.engine;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.Priority;
import com.bumptech.glide.request.ResourceCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class EngineRunnableTest {

  private EngineRunnable.EngineRunnableManager manager;
  private DecodeJob job;
  private Priority priority;
  private EngineRunnable runnable;
  private ResourceCallback callback;

  @Before
  public void setUp() {
    manager = mock(EngineRunnable.EngineRunnableManager.class);
    job = mock(DecodeJob.class);
    priority = Priority.LOW;
    runnable = new EngineRunnable(manager, job, priority);
  }

  @Test
  public void testReturnsGivenPriority() {
    assertEquals(priority.ordinal(), runnable.getPriority());
  }

  @Test
  public void testNotifiesManagerOfResultIfDecodeJobDecodesFromCache() {
    Resource expected = mock(Resource.class);
    when(job.decodeFromCachedResource()).thenReturn(expected);

    runnable.run();

    verify(manager).onResourceReady(eq(expected));
  }

  @Test
  public void testDoesNotNotifyManagerOfFailureIfDecodeJobReturnsNullFromCache() {
    when(job.decodeFromCachedResource()).thenReturn(null);

    runnable.run();

    verify(manager, never()).onLoadFailed();
  }

  @Test
  public void testNotifiesManagerOfResultIfDecodeJobDecodesFromSourceCache() {
    Resource expected = mock(Resource.class);
    when(job.decodeFromCachedData()).thenReturn(expected);

    runnable.run();

    verify(manager).onResourceReady(eq(expected));
  }

  @Test
  public void
  testNotifiesManagerOfResultIfDecodeJobReturnsNullFromCacheButDecodesFromSourceCache()
      throws Exception {
    when(job.decodeFromCachedResource()).thenReturn(null);
    Resource expected = mock(Resource.class);
    when(job.decodeFromCachedData()).thenReturn(expected);

    runnable.run();

    verify(manager).onResourceReady(eq(expected));
  }

  @Test
  public void testDoesNotNotifyManagerOfFailureIfDecodeJobReturnsNullFromDataAndResource() {
    runnable.run();

    verify(manager, never()).onLoadFailed();
  }

  @Test
  public void testSubmitsForSourceIfDecodeJobReturnsNullFromDataAndResource() {
    runnable.run();

    verify(manager).submitForSource(eq(runnable));
  }

  @Test
  public void testNotifiesManagerOfFailureIfJobReturnsNullDecodingFromData() {
    runnable.run();
    runnable.run();

    verify(manager).onLoadFailed();
  }

  @Test
  public void testNotifiesManagerOfFailureIfJobReturnsNullDecodingFromSource() {
    runnable.run();

    when(job.decodeFromSource()).thenReturn(null);
    runnable.run();

    verify(manager).onLoadFailed();
  }

  @Test
  public void testNotifiesManagerOfResultIfDecodeFromSourceSucceeds() {
    runnable.run();

    Resource expected = mock(Resource.class);
    when(job.decodeFromSource()).thenReturn(expected);
    runnable.run();

    verify(manager).onResourceReady(eq(expected));
  }

  @Test
  public void testDoesNotDecodeFromCacheIfCancelled() {
    runnable.cancel();
    runnable.run();

    verify(job, never()).decodeFromCachedResource();
    verify(job, never()).decodeFromCachedData();
  }

  @Test
  public void testDoesNotDecodeFromSourceIfCancelled() {
    runnable.run();
    runnable.cancel();
    runnable.run();

    verify(job, never()).decodeFromSource();
  }

  @Test
  public void testDoesNotRequestSubmitIfCancelled() {
    runnable.cancel();
    runnable.run();

    verify(manager, never()).submitForSource(eq(runnable));
  }

  @Test
  public void testDoesNotNotifyManagerOfFailureIfCancelled() {
    runnable.run();
    when(job.decodeFromSource()).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        runnable.cancel();
        return null;
      }
    });
    runnable.run();

    verify(manager, never()).onLoadFailed();
  }

  @Test
  public void testDoesNotNotifyManagerOfSuccessIfCancelled() {
    runnable.run();
    when(job.decodeFromSource()).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        runnable.cancel();
        return mock(Resource.class);
      }
    });
    runnable.run();

    verify(manager, never()).onResourceReady(any(Resource.class));
  }

  @Test
  public void testRecyclesResourceIfAvailableWhenCancelled() {
    final Resource resource = mock(Resource.class);
    runnable.run();
    when(job.decodeFromSource()).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        runnable.cancel();
        return resource;
      }
    });
    runnable.run();

    verify(resource).recycle();
  }
}