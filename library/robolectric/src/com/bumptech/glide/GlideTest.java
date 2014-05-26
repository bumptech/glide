package com.bumptech.glide;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import com.android.volley.Network;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;
import com.bumptech.glide.loader.GlideUrl;
import com.bumptech.glide.loader.bitmap.model.GenericLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.ModelLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.stream.StreamModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.EngineBuilder;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.Metadata;
import com.bumptech.glide.resize.request.Request;
import com.bumptech.glide.resize.target.Target;
import com.bumptech.glide.volley.VolleyRequestFuture;
import com.bumptech.glide.volley.VolleyUrlLoader;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link Glide} interface and singleton.
 */
@RunWith(RobolectricTestRunner.class)
public class GlideTest {
    private Target target = null;

    @Before
    public void setUp() throws Exception {
        // Ensure that target's size ready callback will be called synchronously.
        target = mock(Target.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Target.SizeReadyCallback cb = (Target.SizeReadyCallback) invocation.getArguments()[0];
                cb.onSizeReady(100, 100);
                return null;
            }
        }).when(target).getSize(any(Target.SizeReadyCallback.class));

        // Run all tasks on the main thread so they complete synchronously.
        ExecutorService service = mock(ExecutorService.class);
        when(service.submit(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return mock(Future.class);
            }
        });

        // Make sure Volley does not actually perform any network requests.
        Network network = mock(Network.class);
        when(network.performRequest(any(com.android.volley.Request.class)))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        return new NetworkResponse(new byte[0]);
                    }
                });

        RequestQueue requestQueue = new RequestQueue(new NoCache(), network);
        requestQueue.start();
        Glide.setup(new GlideBuilder(Robolectric.application)
                .setEngine(new EngineBuilder(Robolectric.application)
                        .setExecutorService(service)
                        .build())
                .setImageManager(new ImageManager.Builder(Robolectric.application)
                        .setResizeService(service)
                        .build())
                .setRequestQueue(requestQueue));

        // Sleep to avoid blocking the main thread while waiting for Volley's background thread to complete
        // and for the result to be posted back to the main thread.
        VolleyUrlLoader.FutureFactory futureFactory = mock(VolleyUrlLoader.FutureFactory.class);
        VolleyRequestFuture<InputStream> future = new VolleyRequestFuture<InputStream>() {
            @Override
            public InputStream get() throws InterruptedException, ExecutionException {
                for (int i = 0; i < 10 && !isDone(); i++) {
                    Thread.sleep(10);
                    // Make sure the result callback posted on the main thread actually runs.
                    Robolectric.runUiThreadTasks();
                }
                if (!isDone()) {
                    Assert.fail("Failed to get response from Volley in time");
                }
                return super.get();
            }
        };
        when(futureFactory.build()).thenReturn(future);
        Glide.get(getContext()).register(GlideUrl.class, InputStream.class,
                new VolleyUrlLoader.Factory(Glide.get(getContext()).getRequestQueue(), futureFactory));
    }

    @After
    public void tearDown() {
        Glide.tearDown();
    }

    @Test
    public void testFileDefaultLoader() {
        File file = new File("fake");
        mockUri(Uri.fromFile(file));

        Glide.with(getContext()).load(file).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test
    public void testUrlDefaultLoader() throws MalformedURLException {
        URL url = new URL("http://www.google.com");

        Glide.with(getContext()).loadFromImage(url).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test
    public void testUriDefaultLoader() {
        Uri uri = Uri.parse("content://test/something");
        mockUri(uri);

        Glide.with(getContext()).load(uri).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test
    public void testStringDefaultLoader() {
        String string = "http://www.google.com";

        Glide.with(getContext()).load(string).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test
    public void testIntegerDefaultLoader() {
        int integer = 1234;
        mockUri("android.resource://" + getContext().getPackageName() + "/" + integer);

        Glide.with(getContext()).load(integer).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test
    public void testByteArrayDefaultLoader() {
        byte[] bytes = new byte[10];
        Glide.with(getContext()).loadFromImage(bytes).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test
    public void testByteArrayWithIdDefaultLoader() {
        byte[] bytes = new byte[10];
        String id = "test";
        Glide.with(getContext()).loadFromImage(bytes, id).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test(expected = Exception.class)
    public void testUnregisteredModelThrowsException() {
        Float unregistered = 0.5f;
        Glide.with(getContext()).load(unregistered).into(target);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnregisteredModelWithGivenLoaderDoesNotThrow() {
        Float unregistered = 0.5f;
        StreamModelLoader<Float> mockLoader = mockStreamModelLoader(Float.class);
        Glide.with(getContext())
                .using(mockLoader)
                .load(unregistered)
                .into(target);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNonDefaultModelWithRegisteredFactoryDoesNotThrow() {
        registerMockStreamModelLoader(Float.class);

        Glide.with(getContext()).load(0.5f).into(target);
    }

    @Test
    public void testNullModelDoesNotThrow() {
        String nullString = null;
        Glide.with(getContext()).load(nullString).into(target);
    }

    private void mockUri(String uriString) {
        mockUri(Uri.parse(uriString));
    }

    private void mockUri(Uri uri) {
        ContentResolver contentResolver = Robolectric.application.getContentResolver();
        Robolectric.shadowOf(contentResolver).registerInputStream(uri, new ByteArrayInputStream(new byte[0]));
    }

    private Context getContext() {
        return Robolectric.application;
    }

    @SuppressWarnings("unchecked")
    private <T> void registerMockStreamModelLoader(final Class<T> modelClass) {
        StreamModelLoader<T> modelLoader = mockStreamModelLoader(modelClass);
        ModelLoaderFactory<T, InputStream> modelLoaderFactory = mock(ModelLoaderFactory.class);
        when(modelLoaderFactory.build(any(Context.class), any(GenericLoaderFactory.class)))
                .thenReturn(modelLoader);

        Glide.get(Robolectric.application).register(modelClass, InputStream.class, modelLoaderFactory);
    }

    @SuppressWarnings("unchecked")
    private <T> StreamModelLoader<T> mockStreamModelLoader(final Class<T> modelClass) {
        StreamModelLoader<T> modelLoader = mock(StreamModelLoader.class);
        ResourceFetcher<InputStream> fetcher = mock(ResourceFetcher.class);
        try {
            when(fetcher.loadResource(any(Metadata.class))).thenReturn(new ByteArrayInputStream(new byte[0]));
        } catch (Exception e) { }
        when(modelLoader.getId(any(modelClass))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                T model = (T) invocation.getArguments()[0];
                return model.toString();
            }
        });
        when(modelLoader.getResourceFetcher(any(modelClass), anyInt(), anyInt()))
                .thenReturn(fetcher);

        return modelLoader;
    }
}

