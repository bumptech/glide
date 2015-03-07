package com.bumptech.glide;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.bytes.BytesResource;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestManagerTreeNode;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.tests.GlideShadowLooper;
import com.bumptech.glide.testutil.TestResourceUtil;

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
import org.robolectric.annotation.Resetter;
import org.robolectric.shadows.ShadowBitmap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Tests for the {@link Glide} interface and singleton.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = { GlideTest.ShadowFileDescriptorContentResolver.class,
        GlideTest.ShadowMediaMetadataRetriever.class, GlideShadowLooper.class, GlideTest.MutableShadowBitmap.class })
public class GlideTest {
    private Target target = null;
    private ImageView imageView;
    private RequestManager requestManager;

    @Before
    public void setUp() throws Exception {
        Glide.tearDown();
        // Ensure that target's size ready callback will be called synchronously.
        target = mock(Target.class);
        imageView = new ImageView(Robolectric.application);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        doAnswer(new CallCallback()).when(target).getSize(any(SizeReadyCallback.class));

        Handler bgHandler = mock(Handler.class);
        when(bgHandler.post(any(Runnable.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return true;
            }
        });

        // Run all tasks on the main thread so they complete synchronously.
        ExecutorService service = mock(ExecutorService.class);
        when(service.submit(any(Runnable.class))).thenAnswer(new Answer<Future<?>>() {
            @Override
            public Future<?> answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return mock(Future.class);
            }
        });

        Glide.setup(new GlideBuilder(Robolectric.application)
                .setMemoryCache(mock(MemoryCache.class))
                .setDiskCache(mock(DiskCache.class))
                .setResizeService(service)
                .setDiskCacheService(service));
        DataFetcher<InputStream> mockStreamFetcher = mock(DataFetcher.class);
        when(mockStreamFetcher.getId()).thenReturn("fakeId");
        when(mockStreamFetcher.loadData(any(Priority.class))).thenReturn(new ByteArrayInputStream(new byte[0]));
        ModelLoader<GlideUrl, InputStream> mockUrlLoader = mock(ModelLoader.class);
        when(mockUrlLoader.getResourceFetcher(any(GlideUrl.class), anyInt(), anyInt())).thenReturn(mockStreamFetcher);
        ModelLoaderFactory<GlideUrl, InputStream> mockUrlLoaderFactory = mock(ModelLoaderFactory.class);
        when(mockUrlLoaderFactory.build(any(Context.class), any(GenericLoaderFactory.class)))
                .thenReturn(mockUrlLoader);

        Glide.get(getContext()).register(GlideUrl.class, InputStream.class, mockUrlLoaderFactory);
        Lifecycle lifecycle = mock(Lifecycle.class);
        RequestManagerTreeNode treeNode = mock(RequestManagerTreeNode.class);
        requestManager = new RequestManager(getContext(), lifecycle, treeNode);
        requestManager.resumeRequests();
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

    @SuppressWarnings("unchecked")
    @Test
    public void testGenericLoader() throws Exception {
        File expected = new File("test");

        Target<File> target = mock(Target.class);
        doAnswer(new CallCallback()).when(target).getSize(any(SizeReadyCallback.class));

        GlideUrl glideUrl =  mock(GlideUrl.class);
        DataFetcher<File> dataFetcher = mock(DataFetcher.class);
        when(dataFetcher.loadData(any(Priority.class))).thenReturn(expected);
        when(dataFetcher.getId()).thenReturn("id");
        ModelLoader<GlideUrl, File> modelLoader = mock(ModelLoader.class);
        when(modelLoader.getResourceFetcher(eq(glideUrl), anyInt(), anyInt()))
                .thenReturn(dataFetcher);

        Resource<File> expectedResource = mock(Resource.class);
        when(expectedResource.get()).thenReturn(expected);
        ResourceDecoder<File, File> sourceDecoder = mock(ResourceDecoder.class);
        when(sourceDecoder.decode(eq(expected), anyInt(), anyInt())).thenReturn(expectedResource);
        when(sourceDecoder.getId()).thenReturn("sourceDecoderId");
        ResourceDecoder<File, File> cacheDecoder = mock(ResourceDecoder.class);
        when(cacheDecoder.getId()).thenReturn("cacheDecoderId");
        ResourceEncoder<File> encoder = mock(ResourceEncoder.class);
        when(encoder.getId()).thenReturn("encoderId");
        Encoder<File> sourceEncoder = mock(Encoder.class);
        when(sourceEncoder.getId()).thenReturn("sourceEncoderId");

        requestManager
                .using(modelLoader, File.class)
                .load(glideUrl)
                .as(File.class)
                .decoder(sourceDecoder)
                .cacheDecoder(cacheDecoder)
                .encoder(encoder)
                .sourceEncoder(sourceEncoder)
                .into(target);

        verify(target).onResourceReady(eq(expected), any(GlideAnimation.class));
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

        requestManager.load(file).into(target);
        requestManager.load(file).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
    }

    @Test
    public void testUrlDefaultLoader() throws MalformedURLException {
        URL url = new URL("http://www.google.com");

        requestManager.load(url).into(target);
        requestManager.load(url).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
    }

    @Test
    public void testAsBitmapOption() {
        Uri uri = Uri.parse("content://something/else");
        mockUri(uri);

        requestManager.load(uri).asBitmap().into(target);

        verify(target).onResourceReady(any(Bitmap.class), any(GlideAnimation.class));
    }

    @Test
    public void testTranscodeOption() {
        Uri uri = Uri.parse("content://something/else");
        mockUri(uri);
        final byte[] bytes = new byte[0];

        ResourceTranscoder<Bitmap, byte[]> transcoder = mock(ResourceTranscoder.class);
        when(transcoder.getId()).thenReturn("bytes");
        when(transcoder.transcode(any(Resource.class))).thenReturn(new BytesResource(bytes));

        requestManager
                .load(uri)
                .asBitmap()
                .transcode(transcoder, byte[].class)
                .into(target);

        verify(target).onResourceReady(eq(bytes), any(GlideAnimation.class));
    }

    @Test
    public void testToBytesOption() {
        Uri uri = Uri.parse("content://something/else");
        mockUri(uri);

        requestManager.load(uri).asBitmap().toBytes().into(target);

        verify(target).onResourceReady(any(byte[].class), any(GlideAnimation.class));
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

        requestManager.load(uri).into(target);
        requestManager.load(uri).into(imageView);

        verify(target).onResourceReady(anyObject(), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
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
    public void testUriStringDefaultLoaderWithFileDescriptor() throws Exception {
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
        requestManager
                .load(string)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target target, boolean isFirstResource) {
                        if (!(e instanceof IOException)) {
                            throw new RuntimeException(e);
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target target,
                            boolean isFromMemoryCache,
                            boolean isFirstResource) {
                        return false;
                    }
                })
                .into(target);
        requestManager.load(string).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
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
        int integer = android.R.drawable.star_on;
        mockUri("android.resource://" + "android" + "/drawable/star_on");

        requestManager.load(integer).into(target);
        requestManager.load(integer).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
    }

    @Test
    public void testByteArrayDefaultLoader() {
        byte[] bytes = new byte[10];
        requestManager.load(bytes).into(target);
        requestManager.load(bytes).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
    }

    @Test
    public void testByteArrayWithIdDefaultLoader() {
        byte[] bytes = new byte[10];
        String id = "test";

        requestManager.load(bytes, id).into(target);
        requestManager.load(bytes, id).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
    }

    @Test(expected = Exception.class)
    public void testUnregisteredModelThrowsException() {
        Float unregistered = 0.5f;
        requestManager.load(unregistered).into(target);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnregisteredModelWithGivenLoaderDoesNotThrow() {
        Float unregistered = 0.5f;
        StreamModelLoader<Float> mockLoader = mockStreamModelLoader(Float.class);
        requestManager
                .using(mockLoader)
                .load(unregistered)
                .into(target);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNonDefaultModelWithRegisteredFactoryDoesNotThrow() {
        registerMockStreamModelLoader(Float.class);

        requestManager.load(0.5f).into(target);
    }

    @Test
    public void testReceivesGif() throws IOException {
        String fakeUri = "content://fake";
        InputStream testGifData = openResource("test.gif");
        mockUri(Uri.parse(fakeUri), testGifData);

        requestManager
                .load(fakeUri)
                .asGif()
                .into(target);

        verify(target).onResourceReady(any(GifDrawable.class), any(GlideAnimation.class));
    }

    @Test
    public void testReceivesGifBytes() throws IOException {
        String fakeUri = "content://fake";
        InputStream testGifData = openResource("test.gif");
        mockUri(Uri.parse(fakeUri), testGifData);

        requestManager
                .load(fakeUri)
                .asGif()
                .toBytes()
                .into(target);

        verify(target).onResourceReady(any(byte[].class), any(GlideAnimation.class));
    }

    @Test
    public void testReceivesBitmapBytes() {
        String fakeUri = "content://fake";
        mockUri(fakeUri);
        requestManager
                .load(fakeUri)
                .asBitmap()
                .toBytes()
                .into(target);

        verify(target).onResourceReady(any(byte[].class), any(GlideAnimation.class));
    }

    @Test
    public void testReceivesTranscodedData() {
        String fakeUri = "content://fake";
        mockUri(fakeUri);
        final Bitmap expected = Bitmap.createBitmap(1234, 6432, Bitmap.Config.ALPHA_8);
        requestManager
                .load(fakeUri)
                .asBitmap()
                .transcode(new ResourceTranscoder<Bitmap, Bitmap>() {
                    @Override
                    public Resource<Bitmap> transcode(Resource<Bitmap> toTranscode) {
                        return new BitmapResource(expected, mock(BitmapPool.class));
                    }

                    @Override
                    public String getId() {
                        return "id";
                    }
                }, Bitmap.class)
                .into(target);

        verify(target).onResourceReady(eq(expected), any(GlideAnimation.class));
    }

    @Test
    public void testReceivesThumbnails() {
        String full = mockUri("content://full");
        String thumb = mockUri("content://thumb");
        requestManager
                .load(full)
                .thumbnail(requestManager
                        .load(thumb))
                .into(target);

        verify(target, times(2)).onResourceReady(any(Drawable.class), any(GlideAnimation.class));
    }

    @Test
    public void testReceivesRecursiveThumbnails() {
        requestManager
                .load(mockUri("content://first"))
                .thumbnail(requestManager
                        .load(mockUri("content://second"))
                        .thumbnail(requestManager
                                .load(mockUri("content://third"))
                                .thumbnail(requestManager
                                        .load(mockUri("content://fourth"))
                                )
                        )
                )
                .into(target);
        verify(target, times(4)).onResourceReady(any(Drawable.class), any(GlideAnimation.class));
    }

    @Test
    public void testReceivesRecursiveThumbnailWithPercentage() {
        requestManager
                .load(mockUri("content://first"))
                .thumbnail(requestManager
                        .load(mockUri("content://second"))
                        .thumbnail(0.5f)
                )
                .into(target);
        verify(target, times(3)).onResourceReady(any(Drawable.class), any(GlideAnimation.class));
    }

    @Test
    public void testNullModelInGenericImageLoadDoesNotThrow() {
        requestManager.load((Double) null).into(target);
    }

    @Test
    public void testNullModelInGenericVideoLoadDoesNotThrow() {
        requestManager.load((Float) null).into(target);
    }

    @Test
    public void testNullModelInGenericLoadDoesNotThrow() {
        requestManager.load((Double) null).into(target);
    }

    @Test
    public void testNullModelDoesNotThrow() {
        String nullString = null;

        Drawable drawable = new ColorDrawable(Color.RED);
        requestManager
                .load(nullString)
                .placeholder(drawable)
                .into(target);

        verify(target).onLoadFailed(any(Exception.class), eq(drawable));
    }

    @Test
    public void testNullModelPrefersErrorDrawable() {
        String nullString = null;

        Drawable placeholder = new ColorDrawable(Color.GREEN);
        Drawable error = new ColorDrawable(Color.RED);

        requestManager
                .load(nullString)
                .placeholder(placeholder)
                .error(error)
                .into(target);

        verify(target).onLoadFailed(any(Exception.class), eq(error));
    }

    @Test
    public void testNullModelWithModelLoaderDoesNotThrow() {
        String nullString = null;
        Drawable drawable = new ColorDrawable(Color.RED);
        StreamModelLoader<String> modelLoader = mock(StreamModelLoader.class);
        requestManager
                .using(modelLoader)
                .load(nullString)
                .placeholder(drawable)
                .into(target);

        verify(target).onLoadFailed(any(Exception.class), eq(drawable));
    }

    @Test
    public void testByteData() {
        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6 };
        requestManager.load(data).into(target);
    }

    @Test
    public void testClone() throws IOException {
        GlideDrawable firstResult = mock(GlideDrawable.class);
        Resource<GlideDrawable> firstResource = mock(Resource.class);
        when(firstResource.get()).thenReturn(firstResult);
        ResourceTranscoder<GifBitmapWrapper, GlideDrawable> firstTranscoder = mock(ResourceTranscoder.class);
        when(firstTranscoder.transcode(any(Resource.class))).thenReturn(firstResource);
        when(firstTranscoder.getId()).thenReturn("transcoder1");

        GlideDrawable secondResult = mock(GlideDrawable.class);
        Resource<GlideDrawable> secondResource = mock(Resource.class);
        when(secondResource.get()).thenReturn(secondResult);
        ResourceTranscoder<GifBitmapWrapper, GlideDrawable> secondTranscoder = mock(ResourceTranscoder.class);
        when(secondTranscoder.transcode(any(Resource.class))).thenReturn(secondResource);
        when(secondTranscoder.getId()).thenReturn("transcoder2");

        DrawableRequestBuilder<String> firstRequest = requestManager.from(String.class).transcoder(firstTranscoder)
                .override(100, 100);
        DrawableRequestBuilder<String> secondRequest = firstRequest.clone().transcoder(secondTranscoder);

        Target firstTarget = mock(Target.class);
        Target secondTarget = mock(Target.class);

        String fakeUri = mockUri("content://fakeUri");

        firstRequest.load(fakeUri).into(firstTarget);
        verify(firstTarget).onResourceReady(eq(firstResult), any(GlideAnimation.class));

        secondRequest.load(fakeUri).into(secondTarget);
        verify(secondTarget).onResourceReady(eq(secondResult), any(GlideAnimation.class));
    }

    @SuppressWarnings("unchecked")
    private <T, Z> void registerFailFactory(Class<T> failModel, Class<Z> failResource) throws Exception {
        DataFetcher<Z> failFetcher = mock(DataFetcher.class);
        when(failFetcher.loadData(any(Priority.class))).thenThrow(new IOException("test"));
        when(failFetcher.getId()).thenReturn("fakeId");
        ModelLoader<T, Z> failLoader = mock(ModelLoader.class);
        when(failLoader.getResourceFetcher(any(failModel), anyInt(), anyInt())).thenReturn(failFetcher);
        ModelLoaderFactory<T, Z> failFactory = mock(ModelLoaderFactory.class);
        when(failFactory.build(any(Context.class), any(GenericLoaderFactory.class))).thenReturn(failLoader);

        Glide.get(getContext()).register(failModel, failResource, failFactory);
    }

    private String mockUri(String uriString) {
        return mockUri(Uri.parse(uriString), null);
    }

    private String mockUri(Uri uri) {
        return mockUri(uri, null);
    }

    private String mockUri(Uri uri, InputStream is) {
        if (is == null) {
            is = new ByteArrayInputStream(new byte[0]);
        }
        ContentResolver contentResolver = Robolectric.application.getContentResolver();
        ShadowFileDescriptorContentResolver shadowContentResolver = Robolectric.shadowOf_(contentResolver);
        shadowContentResolver.registerInputStream(uri, is);

        AssetFileDescriptor assetFileDescriptor = mock(AssetFileDescriptor.class);
        ParcelFileDescriptor parcelFileDescriptor = mock(ParcelFileDescriptor.class);
        when(assetFileDescriptor.getParcelFileDescriptor()).thenReturn(parcelFileDescriptor);

        shadowContentResolver.registerAssetFileDescriptor(uri, assetFileDescriptor);
        return uri.toString();
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
        DataFetcher<InputStream> fetcher = mock(DataFetcher.class);
        try {
            when(fetcher.loadData(any(Priority.class))).thenReturn(new ByteArrayInputStream(new byte[0]));
        } catch (Exception e) {
            // Do nothing.
        }
        when(fetcher.getId()).thenReturn(UUID.randomUUID().toString());
        when(modelLoader.getResourceFetcher(any(modelClass), anyInt(), anyInt()))
                .thenReturn(fetcher);

        return modelLoader;
    }


    private InputStream openResource(String imageName) throws IOException {
        return TestResourceUtil.openResource(getClass(), imageName);
    }

    private static class CallCallback implements Answer<Void> {
        private int width;
        private int height;

        public CallCallback() {
            this(100, 100);
        }

        public CallCallback(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            SizeReadyCallback cb = (SizeReadyCallback) invocation.getArguments()[0];
            cb.onSizeReady(width, height);
            return null;
        }
    }

    // TODO: Extending ShadowContentResolver results in exceptions because of some state issues where we seem to get
    // one content resolver shadow in one part of the test and a different one in a different part of the test. Each
    // one ends up with different registered uris, which causes tests to fail. We shouldn't need to do this, but
    // using static maps seems to fix the issue.
    @Implements(value = ContentResolver.class)
    public static class ShadowFileDescriptorContentResolver {
        private static final Map<Uri, AssetFileDescriptor> URI_TO_FILE_DESCRIPTOR =
                new HashMap<Uri, AssetFileDescriptor>();
        private static final Map<Uri, InputStream> URI_TO_INPUT_STREAMS = new HashMap<Uri, InputStream>();

        public void registerInputStream(Uri uri, InputStream inputStream) {
            URI_TO_INPUT_STREAMS.put(uri, inputStream);
        }

        public void registerAssetFileDescriptor(Uri uri, AssetFileDescriptor assetFileDescriptor) {
            URI_TO_FILE_DESCRIPTOR.put(uri, assetFileDescriptor);
        }

        @Implementation
        public InputStream openInputStream(Uri uri) {
            if (!URI_TO_INPUT_STREAMS.containsKey(uri)) {
                throw new IllegalArgumentException("You must first register an InputStream for uri: " + uri);
            }
            return URI_TO_INPUT_STREAMS.get(uri);
        }

        @Implementation
        public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String type) {
            if (!URI_TO_FILE_DESCRIPTOR.containsKey(uri)) {
                throw new IllegalArgumentException("You must first register an AssetFileDescriptor for uri: " + uri);
            }
            return URI_TO_FILE_DESCRIPTOR.get(uri);
        }

        @Resetter
        public static void reset() {
            URI_TO_INPUT_STREAMS.clear();
            URI_TO_FILE_DESCRIPTOR.clear();
        }
    }

    @Implements(Bitmap.class)
    public static class MutableShadowBitmap extends ShadowBitmap {

        @Implementation
        public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
            Bitmap bitmap = ShadowBitmap.createBitmap(width, height, config);
            Robolectric.shadowOf(bitmap).setMutable(true);
            return bitmap;
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

