package com.bumptech.glide;

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
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.GlideAnimation;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.tests.GlideShadowLooper;
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

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
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
@Config(shadows = { GlideTest.ShadowFileDescriptorContentResolver.class, GlideTest.ShadowMediaMetadataRetriever.class,
        GlideShadowLooper.class })
public class GlideTest {
    private Target target = null;
    private ImageView imageView;

    @Before
    public void setUp() throws Exception {
        Glide.tearDown();
        // Ensure that target's size ready callback will be called synchronously.
        target = mock(Target.class);
        imageView = new ImageView(Robolectric.application);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        doAnswer(new CallCallback()).when(target).getSize(any(Target.SizeReadyCallback.class));

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
        doAnswer(new CallCallback()).when(target).getSize(any(Target.SizeReadyCallback.class));

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

        Glide.with(getContext())
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

        Glide.with(getContext()).load(file).into(target);
        Glide.with(getContext()).load(file).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
    }

    @Test
    public void testUrlDefaultLoader() throws MalformedURLException {
        URL url = new URL("http://www.google.com");

        Glide.with(getContext()).loadFromImage(url).into(target);
        Glide.with(getContext()).loadFromImage(url).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
    }

    @Test
    public void testAsBitmapOption() {
        Uri uri = Uri.parse("content://something/else");
        mockUri(uri);

        Glide.with(getContext()).load(uri).asBitmap().into(target);

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

        Glide.with(getContext())
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

        Glide.with(getContext()).load(uri).asBitmap().toBytes().into(target);

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

        Glide.with(getContext()).load(uri).into(target);
        Glide.with(getContext()).load(uri).into(imageView);

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
        Glide.with(getContext())
                .load(string)
                .listener(new RequestListener<String, Drawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target target, boolean isFirstImage) {
                        if (!(e instanceof IOException)) {
                            throw new RuntimeException(e);
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, String model, Target target,
                            boolean isFromMemoryCache,
                            boolean isFirstResource) {
                        return false;
                    }
                })
                .into(target);
        Glide.with(getContext()).load(string).into(imageView);

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
        int integer = 1234;
        mockUri("android.resource://" + getContext().getPackageName() + "/" + integer);

        Glide.with(getContext()).load(integer).into(target);
        Glide.with(getContext()).load(integer).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
    }

    @Test
    public void testByteArrayDefaultLoader() {
        byte[] bytes = new byte[10];
        Glide.with(getContext()).loadFromImage(bytes).into(target);
        Glide.with(getContext()).loadFromImage(bytes).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
    }

    @Test
    public void testByteArrayWithIdDefaultLoader() {
        byte[] bytes = new byte[10];
        String id = "test";

        Glide.with(getContext()).loadFromImage(bytes, id).into(target);
        Glide.with(getContext()).loadFromImage(bytes, id).into(imageView);

        verify(target).onResourceReady(any(Resource.class), any(GlideAnimation.class));
        verify(target).setRequest((Request) notNull());

        assertNotNull(imageView.getDrawable());
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
    public void testReceivesGif() {
        String fakeUri = "content://fake";
        mockUri(fakeUri);
        Glide.with(getContext())
                .load(fakeUri)
                .asGif()
                .into(target);
        verify(target).onResourceReady(any(GifDrawable.class), any(GlideAnimation.class));
    }

    @Test
    public void testReceivesGifBytes() {
        String fakeUri = "content://fake";
        mockUri(fakeUri);
        Glide.with(getContext())
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
        Glide.with(getContext())
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
        Glide.with(getContext())
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
    public void testNullModelInGenericImageLoadDoesNotThrow() {
        Glide.with(getContext()).loadFromImage((Double) null).into(target);
    }

    @Test
    public void testNullModelInGenericVideoLoadDoesNotThrow() {
        Glide.with(getContext()).loadFromVideo((Float) null).into(target);
    }

    @Test
    public void testNullModelInGenericLoadDoesNotThrow() {
        Glide.with(getContext()).load((Double) null).into(target);
    }

    @Test
    public void testNullModelDoesNotThrow() {
        String nullString = null;

        Drawable drawable = new ColorDrawable(Color.RED);
        Glide.with(getContext())
                .load(nullString)
                .placeholder(drawable)
                .into(target);

        verify(target).setPlaceholder(eq(drawable));
    }

    @Test
    public void testNullModelPrefersErrorDrawable() {
        String nullString = null;

        Drawable placeholder = new ColorDrawable(Color.GREEN);
        Drawable error = new ColorDrawable(Color.RED);

        Glide.with(getContext())
                .load(nullString)
                .placeholder(placeholder)
                .error(error)
                .into(target);

        verify(target).setPlaceholder(eq(error));
    }

    @Test
    public void testNullModelWithModelLoaderDoesNotThrow() {
        String nullString = null;
        Drawable drawable = new ColorDrawable(Color.RED);
        StreamModelLoader<String> modelLoader = mock(StreamModelLoader.class);
        Glide.with(getContext())
                .using(modelLoader)
                .load(nullString)
                .placeholder(drawable)
                .into(target);

        verify(target).setPlaceholder(eq(drawable));
    }

    private void mockUri(String uriString) {
        mockUri(Uri.parse(uriString));
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
        DataFetcher<InputStream> fetcher = mock(DataFetcher.class);
        try {
            when(fetcher.loadData(any(Priority.class))).thenReturn(new ByteArrayInputStream(new byte[0]));
        } catch (Exception e) { }
        when(fetcher.getId()).thenReturn(UUID.randomUUID().toString());
        when(modelLoader.getResourceFetcher(any(modelClass), anyInt(), anyInt()))
                .thenReturn(fetcher);

        return modelLoader;
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
            Target.SizeReadyCallback cb = (Target.SizeReadyCallback) invocation.getArguments()[0];
            cb.onSizeReady(width, height);
            return null;
        }
    }

    // TODO: Extending ShadowContentResolver results in exceptions because of some state issues where we seem to get
    // one content resolver shadow in one part of the test and a different one in a different part of the test. Each
    // one ends up with different registered uris, which causes tests to fail. We shouldn't need to do this, but
    // using static maps seems to fix the issue.
    @Implements(value = ContentResolver.class, resetStaticState = true)
    public static class ShadowFileDescriptorContentResolver {
        private static final Map<Uri, AssetFileDescriptor> uriToFileDescriptors = new HashMap<Uri, AssetFileDescriptor>();
        private static final Map<Uri, InputStream> uriToInputStreams = new HashMap<Uri, InputStream>();

        public void registerInputStream(Uri uri, InputStream inputStream) {
            uriToInputStreams.put(uri, inputStream);
        }

        public void registerAssetFileDescriptor(Uri uri, AssetFileDescriptor assetFileDescriptor) {
            uriToFileDescriptors.put(uri, assetFileDescriptor);
        }

        @Implementation
        public InputStream openInputStream(Uri uri) {
            if (!uriToInputStreams.containsKey(uri)) {
                throw new IllegalArgumentException("You must first register an InputStream for uri: " + uri);
            }
            return uriToInputStreams.get(uri);
        }

        @Implementation
        public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String type) {
            if (!uriToFileDescriptors.containsKey(uri)) {
                throw new IllegalArgumentException("You must first register an AssetFileDescriptor for uri: " + uri);
            }
            return uriToFileDescriptors.get(uri);
        }

        public static void reset() {
            uriToInputStreams.clear();
            uriToFileDescriptors.clear();
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

