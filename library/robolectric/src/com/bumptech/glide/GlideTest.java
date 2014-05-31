package com.bumptech.glide;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import com.android.volley.Network;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;
import com.bumptech.glide.load.resource.ResourceFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.load.engine.EngineBuilder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.Target;
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
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link Glide} interface and singleton.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = { GlideTest.ShadowFileDescriptorContentResolver.class, GlideTest.ShadowMediaMetadataRetriever.class })
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

        Handler bgHandler = mock(Handler.class);
        when(bgHandler.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return true;
            }
        });

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
                .setEngine(new EngineBuilder(mock(MemoryCache.class), mock(DiskCache.class))
                        .setExecutorService(service)
                        .setBackgroundHandler(bgHandler)
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
    public void testCanSetMemoryCategory() {
        MemoryCache memoryCache = mock(MemoryCache.class);
        BitmapPool bitmapPool = mock(BitmapPool.class);

        MemoryCategory memoryCategory = MemoryCategory.NORMAL;
        Glide glide = new GlideBuilder(getContext())
                .setMemoryCache(memoryCache)
                .setBitmapPool(bitmapPool)
                .createGlide();
        glide.setMemoryCategory(memoryCategory);

        verify(memoryCache).setSizeMultiplier(eq(memoryCategory.getMultiplier()));
        verify(bitmapPool).setSizeMultiplier(eq(memoryCategory.getMultiplier()));
    }

    @Test
    public void testClearMemory() {
        BitmapPool bitmapPool = mock(BitmapPool.class);
        MemoryCache memoryCache = mock(MemoryCache.class);

        Glide glide = new GlideBuilder(getContext())
                .setBitmapPool(bitmapPool)
                .setMemoryCache(memoryCache)
                .createGlide();

        glide.clearMemory();

        verify(bitmapPool).clearMemory();
        verify(memoryCache).clearMemory();
    }

    @Test
    public void testTrimMemory() {
        BitmapPool bitmapPool = mock(BitmapPool.class);
        MemoryCache memoryCache = mock(MemoryCache.class);

        Glide glide = new GlideBuilder(getContext())
                .setBitmapPool(bitmapPool)
                .setMemoryCache(memoryCache)
                .createGlide();

        final int level = 123;

        glide.trimMemory(level);

        verify(bitmapPool).trimMemory(eq(level));
        verify(memoryCache).trimMemory(eq(level));
    }

    @Test
    public void testFileDefaultLoaderWithInputStream() throws Exception {
        registerFailFactory(File.class, ParcelFileDescriptor.class);
        runTestFileDefaultLoader();
    }

    @Test
    public void testFileDefaultLoaderWithFileDescriptor() throws Exception {
        registerFailFactory(File.class, InputStream.class);
        runTestFileDefaultLoader();
    }

    @Test
    public void testFileDefaultLoader() {
        runTestFileDefaultLoader();
    }

    private void runTestFileDefaultLoader() {
        File file = new File("fake");
        mockUri(Uri.fromFile(file));

        Glide.with(getContext()).load(file).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test
    public void runTestUrlDefaultLoader() throws MalformedURLException {
        URL url = new URL("http://www.google.com");

        Glide.with(getContext()).loadFromImage(url).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test
    public void testUriDefaultLoaderWithInputStream() throws Exception {
        registerFailFactory(Uri.class, ParcelFileDescriptor.class);
        runTestUriDefaultLoader();
    }

    @Test
    public void testUriDefaultLoaderWithFileDescriptor() throws Exception {
        registerFailFactory(Uri.class, InputStream.class);
        runTestUriDefaultLoader();
    }

    @Test
    public void testUriDefaultLoader() {
        runTestUriDefaultLoader();
    }

    private void runTestUriDefaultLoader() {
        Uri uri = Uri.parse("content://test/something");
        mockUri(uri);

        Glide.with(getContext()).load(uri).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test
    public void testStringDefaultLoaderWithUrl() {
        runTestStringDefaultLoader("http://www.google.com");
    }

    @Test
    public void testFileStringDefaultLoaderWithInputStream() throws Exception {
        registerFailFactory(String.class, ParcelFileDescriptor.class);
        runTestFileStringDefaultLoader();
    }

    @Test
    public void testFileStringDefaultLoaderWithFileDescriptor() throws Exception {
        registerFailFactory(String.class, ParcelFileDescriptor.class);
        runTestFileStringDefaultLoader();
    }

    @Test
    public void testFileStringDefaultLoader() {
        runTestFileStringDefaultLoader();
    }

    private void runTestFileStringDefaultLoader() {
        String path = "/some/random/path";
        mockUri(Uri.fromFile(new File(path)));
        runTestStringDefaultLoader(path);
    }

    @Test
    public void testUriStringDefaultLoaderWithInputStream() throws Exception {
        registerFailFactory(String.class, ParcelFileDescriptor.class);
        runTestUriStringDefaultLoader();
    }

    @Test
    public void testUriStringDefaultLoaerWithFileDescriptor() throws Exception {
        registerFailFactory(String.class, InputStream.class);
        runTestUriStringDefaultLoader();
    }

    @Test
    public void testUriStringDefaultLoader() {
        runTestUriStringDefaultLoader();
    }

    private void runTestUriStringDefaultLoader() {
        String stringUri = "content://some/random/uri";
        mockUri(Uri.parse(stringUri));
        runTestStringDefaultLoader(stringUri);
    }

    private void runTestStringDefaultLoader(String string) {
        Glide.with(getContext()).load(string).into(target);

        verify(target).onImageReady(any(Bitmap.class));
        verify(target).setRequest((Request) notNull());
    }

    @Test
    public void testIntegerDefaultLoaderWithInputStream() throws Exception {
        registerFailFactory(Integer.class, ParcelFileDescriptor.class);
        runTestIntegerDefaultLoader();
    }

    @Test
    public void testIntegerDefaultLoaderWithFileDescriptor() throws Exception {
        registerFailFactory(Integer.class, InputStream.class);
        runTestIntegerDefaultLoader();
    }

    @Test
    public void testIntegerDefaultLoader() {
        runTestIntegerDefaultLoader();
    }

    private void runTestIntegerDefaultLoader() {
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
        Drawable drawable = mock(Drawable.class);
        Glide.with(getContext())
                .load(nullString)
                .placeholder(drawable)
                .into(target);

        verify(target).setPlaceholder(drawable);
    }

    private void mockUri(String uriString) {
        mockUri(Uri.parse(uriString));
    }

    @SuppressWarnings("unchecked")
    private <T, Z> void registerFailFactory(Class<T> failModel, Class<Z> failResource) throws Exception {
        ResourceFetcher<Z> failFetcher = mock(ResourceFetcher.class);
        when(failFetcher.loadResource(any(Priority.class))).thenThrow(new IOException("test"));
        ModelLoader<T, Z> failLoader = mock(ModelLoader.class);
        when(failLoader.getId(any(failModel))).thenReturn("fakeId");
        when(failLoader.getResourceFetcher(any(failModel), anyInt(), anyInt())).thenReturn(failFetcher);
        ModelLoaderFactory<T, Z> failFactory = mock(ModelLoaderFactory.class);
        when(failFactory.build(any(Context.class), any(GenericLoaderFactory.class))).thenReturn(failLoader);

        Glide.get(getContext()).register(failModel, failResource, failFactory);
    }


    private void mockUri(Uri uri) {
        ContentResolver contentResolver = Robolectric.application.getContentResolver();
        ShadowFileDescriptorContentResolver shadowContentResolver = Robolectric.shadowOf_(contentResolver);
        shadowContentResolver.registerInputStream(uri, new ByteArrayInputStream(new byte[0]));
        AssetFileDescriptor assetFileDescriptor = mock(AssetFileDescriptor.class);
        ParcelFileDescriptor parcelFileDescriptor = mock(ParcelFileDescriptor.class);
        when(assetFileDescriptor.getParcelFileDescriptor()).thenReturn(parcelFileDescriptor);
        shadowContentResolver.registerAssetFileDescriptor(uri, assetFileDescriptor);
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
            when(fetcher.loadResource(any(Priority.class))).thenReturn(new ByteArrayInputStream(new byte[0]));
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

    @Implements(ContentResolver.class)
    public static class ShadowFileDescriptorContentResolver extends ShadowContentResolver {
        private final Map<Uri, AssetFileDescriptor> uriToFileDescriptors = new HashMap<Uri, AssetFileDescriptor>();

        public void registerAssetFileDescriptor(Uri uri, AssetFileDescriptor assetFileDescriptor) {
            uriToFileDescriptors.put(uri, assetFileDescriptor);
        }

        @Implementation
        @SuppressWarnings("unused")
        public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String type) {
            if (!uriToFileDescriptors.containsKey(uri)) {
                throw new IllegalArgumentException("You must first register an AssetFileDescriptor for uri: " + uri);
            }
            return uriToFileDescriptors.get(uri);
        }
    }

    @Implements(MediaMetadataRetriever.class)
    public static class ShadowMediaMetadataRetriever {

        @Implementation
        @SuppressWarnings("unused")
        public Bitmap getFrameAtTime() {
            Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            Robolectric.shadowOf(bitmap).appendDescription(" from MediaMetadataRetriever");
            return bitmap;
        }
    }
}

