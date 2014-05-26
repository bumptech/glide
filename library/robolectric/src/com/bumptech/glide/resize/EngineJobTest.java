package com.bumptech.glide.resize;

import android.os.Handler;
import com.bumptech.glide.resize.cache.ResourceCache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class EngineJobTest {
    private static final String ID = "asdfas";

    @Test
    public void testOnResourceReadyPassedToCallbacks() throws Exception {
        Handler mainHandler = new Handler();

        ResourceCallback cb = mock(ResourceCallback.class);
        Resource resource = mock(Resource.class);

        EngineJob engineJob = new EngineJob(ID, mock(ResourceCache.class), getRunners(), mainHandler);
        engineJob.addCallback(cb);
        engineJob.onResourceReady(resource);

        Robolectric.runUiThreadTasks();
        verify(cb).onResourceReady(eq(resource));
    }

    @Test
    public void testRunnerRemovedFromMapOnResourceReady() {
        Map<String, ResourceRunner> map = getRunners();

        EngineJob engineJob = new EngineJob(ID, mock(ResourceCache.class), map, new Handler());
        engineJob.onResourceReady(mock(Resource.class));

        Robolectric.runUiThreadTasks();
        assertFalse(map.containsKey(ID));
    }

    @Test
    public void testResourceAddedToCacheOnResoureReady() {
        ResourceCache resourceCache = mock(ResourceCache.class);
        Resource result = mock(Resource.class);

        EngineJob engineJob = new EngineJob(ID, resourceCache, getRunners(), new Handler());
        engineJob.onResourceReady(result);

        Robolectric.runUiThreadTasks();
        verify(resourceCache).put(eq(ID), eq(result));
    }

    @Test
    public void testOnExceptionPassedToCallbacks() throws Exception {
        Handler mainHandler = new Handler();

        ResourceCallback cb = mock(ResourceCallback.class);
        Exception exception = new Exception("Test");

        EngineJob engineJob = new EngineJob(ID, mock(ResourceCache.class), getRunners(), mainHandler);
        engineJob.addCallback(cb);
        engineJob.onException(exception);

        Robolectric.runUiThreadTasks();
        verify(cb).onException(eq(exception));
    }

    @Test
    public void testRunnerRemovedFromMapOnException() {
        Map<String, ResourceRunner> map = getRunners();

        EngineJob engineJob = new EngineJob(ID, mock(ResourceCache.class), map, new Handler());
        engineJob.onException(new Exception("test"));

        Robolectric.runUiThreadTasks();
        assertFalse(map.containsKey(ID));
    }

    @Test
    public void testRunnerRemovedFromMapOnCancel() {
        Map<String, ResourceRunner> map = getRunners();

        EngineJob engineJob = new EngineJob(ID, mock(ResourceCache.class), map, new Handler());
        engineJob.cancel();

        assertFalse(map.containsKey(ID));
    }

    @Test
    public void testOnResourceReadyNotDeliveredAfterCancel() {
        EngineJob engineJob = new EngineJob(ID, mock(ResourceCache.class), getRunners(), new Handler());
        ResourceCallback cb = mock(ResourceCallback.class);
        engineJob.addCallback(cb);
        engineJob.cancel();

        engineJob.onResourceReady(mock(Resource.class));

        Robolectric.runUiThreadTasks();
        verify(cb, never()).onResourceReady(any(Resource.class));
    }

    @Test
    public void testOnExceptionNotDeliveredAfterCancel() {
        EngineJob engineJob = new EngineJob(ID, mock(ResourceCache.class), getRunners(), new Handler());
        ResourceCallback cb = mock(ResourceCallback.class);
        engineJob.addCallback(cb);
        engineJob.cancel();

        engineJob.onException(new Exception("test"));

        Robolectric.runUiThreadTasks();
        verify(cb, never()).onException(any(Exception.class));
    }

    @Test
    public void testRunnerReceivesCancel() {
        ResourceRunner runner = mock(ResourceRunner.class);
        Map<String, ResourceRunner> runners = getRunners(runner);
        runners.put(ID, runner);

        EngineJob engineJob = new EngineJob(ID, mock(ResourceCache.class), runners, new Handler());
        engineJob.cancel();

        verify(runner).cancel();
    }

    private Map<String, ResourceRunner> getRunners() {
        return getRunners(mock(ResourceRunner.class));
    }

    private Map<String, ResourceRunner> getRunners(ResourceRunner resourceRunner) {
        Map<String, ResourceRunner> result = new HashMap<String, ResourceRunner>();
        result.put(ID, resourceRunner);
        return result;
    }
}
