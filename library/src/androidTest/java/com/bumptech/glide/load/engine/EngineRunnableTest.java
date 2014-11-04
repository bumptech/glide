package com.bumptech.glide.load.engine;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
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
    public void testNotifiesManagerOfResultIfDecodeJobDecodesFromCache() throws Exception {
        Resource expected = mock(Resource.class);
        when(job.decodeResultFromCache()).thenReturn(expected);

        runnable.run();

        verify(manager).onResourceReady(eq(expected));
    }

    @Test
    public void testDoesNotNotifyManagerOfFailureIfDecodeJobReturnsNullFromCache() throws Exception {
        when(job.decodeResultFromCache()).thenReturn(null);

        runnable.run();

        verify(manager, never()).onException(any(Exception.class));
    }

    @Test
    public void testDoesNotNotifyManagerOfFailureIfDecodeJobThrowsExceptionFromCache() throws Exception {
        when(job.decodeResultFromCache()).thenThrow(new RuntimeException("test"));

        runnable.run();

        verify(manager, never()).onException(any(Exception.class));
    }

    @Test
    public void testNotifiesManagerOfResultIfDecodeJobDecodesFromSourceCache() throws Exception {
        Resource expected = mock(Resource.class);
        when(job.decodeSourceFromCache()).thenReturn(expected);

        runnable.run();

        verify(manager).onResourceReady(eq(expected));
    }

    @Test
    public void testNotifiesManagerOfResultIfDecodeJobThrowsGettingResultFromCacheButDecodesFromSourceCache()
            throws Exception {
        when(job.decodeResultFromCache()).thenThrow(new RuntimeException("test"));
        Resource expected = mock(Resource.class);
        when(job.decodeSourceFromCache()).thenReturn(expected);

        runnable.run();

        verify(manager).onResourceReady(eq(expected));
    }

    @Test
    public void testDoesNotNotifyManagerOfFailureIfDecodeJobReturnsNullFromSourceAndResultCache() {
        runnable.run();

        verify(manager, never()).onException(any(Exception.class));
    }

    @Test
    public void testDoesNotNotifyManagerOfFailureIfDecodeJobThrowsFromSourceAndResultCache() throws Exception {
        when(job.decodeResultFromCache()).thenThrow(new RuntimeException("test"));
        when(job.decodeSourceFromCache()).thenThrow(new RuntimeException("test"));

        runnable.run();

        verify(manager, never()).onException(any(Exception.class));
    }

    @Test
    public void testSubmitsForSourceIfDecodeJobReturnsNullFromSourceAndResultCache() {
        runnable.run();

        verify(manager).submitForSource(eq(runnable));
    }

    @Test
    public void testSubmitsForSourceIfDecodeJobThrowsFromSourceAndResultCache() throws Exception {
        when(job.decodeResultFromCache()).thenThrow(new RuntimeException("test"));
        when(job.decodeSourceFromCache()).thenThrow(new RuntimeException("test"));

        runnable.run();

        verify(manager).submitForSource(eq(runnable));
    }

    @Test
    public void testNotifiesManagerOfFailureIfJobReturnsNullDecodingFromSource() {
        runnable.run();
        runnable.run();

        verify(manager).onException((Exception) isNull());
    }

    @Test
    public void testNotifiesManagerOfFailureIfJobThrowsDecodingFromSource() throws Exception {
        runnable.run();

        Exception expected = new RuntimeException("test");
        when(job.decodeFromSource()).thenThrow(expected);
        runnable.run();

        verify(manager).onException(eq(expected));
    }

    @Test
    public void testNotifiesManagerOfResultIfDecodeFromSourceSucceeds() throws Exception {
        runnable.run();

        Resource expected = mock(Resource.class);
        when(job.decodeFromSource()).thenReturn(expected);
        runnable.run();

        verify(manager).onResourceReady(eq(expected));
    }

    @Test
    public void testDoesNotDecodeFromCacheIfCancelled() throws Exception {
        runnable.cancel();
        runnable.run();

        verify(job, never()).decodeResultFromCache();
        verify(job, never()).decodeSourceFromCache();
    }

    @Test
    public void testDoesNotDecodeFromSourceIfCancelled() throws Exception {
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
    public void testDoesNotNotifyManagerOfFailureIfCancelled() throws Exception {
        runnable.run();
        when(job.decodeFromSource()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                runnable.cancel();
                throw new RuntimeException("test");
            }
        });
        runnable.run();

        verify(manager, never()).onException(any(Exception.class));
    }

    @Test
    public void testDoesNotNotifyManagerOfSuccessIfCancelled() throws Exception {
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
    public void testRecyclesResourceIfAvailableWhenCancelled() throws Exception {
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