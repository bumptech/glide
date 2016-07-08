package com.bumptech.glide;

import static com.bumptech.glide.request.RequestOptions.decodeTypeOf;
import static com.bumptech.glide.request.RequestOptions.placeholderOf;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestManagerTreeNode;
import com.bumptech.glide.module.GlideModule;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.tests.GlideShadowLooper;
import com.bumptech.glide.tests.Util;
import com.bumptech.glide.testutil.TestResourceUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowBitmap;

/**
 * Tests for the {@link Glide} interface and singleton.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18, shadows = {
    GlideTest.ShadowFileDescriptorContentResolver.class,
    GlideTest.ShadowMediaMetadataRetriever.class, GlideShadowLooper.class,
    GlideTest.MutableShadowBitmap.class })
@SuppressWarnings("unchecked")
public class GlideTest {
  @SuppressWarnings("rawtypes")
  private Target target = null;
  private ImageView imageView;
  private RequestManager requestManager;

  @Before
  public void setUp() throws Exception {
    Glide.tearDown();

    RobolectricPackageManager pm =
        (RobolectricPackageManager) RuntimeEnvironment.application.getPackageManager();
    ApplicationInfo info =
        pm.getApplicationInfo(RuntimeEnvironment.application.getPackageName(), 0);
    info.metaData = new Bundle();
    info.metaData.putString(SetupModule.class.getName(), "GlideModule");

    // Ensure that target's size ready callback will be called synchronously.
    target = mock(Target.class);
    imageView = new ImageView(RuntimeEnvironment.application);
    imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
    doAnswer(new CallSizeReady()).when(target).getSize(isA(SizeReadyCallback.class));

    Handler bgHandler = mock(Handler.class);
    when(bgHandler.post(isA(Runnable.class))).thenAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        Runnable runnable = (Runnable) invocation.getArguments()[0];
        runnable.run();
        return true;
      }
    });

    Lifecycle lifecycle = mock(Lifecycle.class);
    RequestManagerTreeNode treeNode = mock(RequestManagerTreeNode.class);
    requestManager = new RequestManager(Glide.get(getContext()), lifecycle, treeNode);
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
    Glide glide =
        new GlideBuilder(getContext()).setMemoryCache(memoryCache).setBitmapPool(bitmapPool)
            .createGlide();
    glide.setMemoryCategory(memoryCategory);

    verify(memoryCache).setSizeMultiplier(eq(memoryCategory.getMultiplier()));
    verify(bitmapPool).setSizeMultiplier(eq(memoryCategory.getMultiplier()));
  }

  @Test
  public void testClearMemory() {
    BitmapPool bitmapPool = mock(BitmapPool.class);
    MemoryCache memoryCache = mock(MemoryCache.class);

    Glide glide =
        new GlideBuilder(getContext()).setBitmapPool(bitmapPool).setMemoryCache(memoryCache)
            .createGlide();

    glide.clearMemory();

    verify(bitmapPool).clearMemory();
    verify(memoryCache).clearMemory();
  }

  @Test
  public void testTrimMemory() {
    BitmapPool bitmapPool = mock(BitmapPool.class);
    MemoryCache memoryCache = mock(MemoryCache.class);

    Glide glide =
        new GlideBuilder(getContext()).setBitmapPool(bitmapPool).setMemoryCache(memoryCache)
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

    requestManager.load(file).into(target);
    requestManager.load(file).into(imageView);

    verify(target).onResourceReady(isA(BitmapDrawable.class), isA(Transition.class));
    verify(target).setRequest((Request) notNull());

    assertNotNull(imageView.getDrawable());
  }

  @Test
  public void testUrlDefaultLoader() throws MalformedURLException {
    URL url = new URL("http://www.google.com");

    requestManager.load(url).into(target);
    requestManager.load(url).into(imageView);

    verify(target).onResourceReady(isA(BitmapDrawable.class), isA(Transition.class));
    verify(target).setRequest((Request) notNull());

    assertNotNull(imageView.getDrawable());
  }

  @Test
  public void testAsBitmapOption() {
    Uri uri = Uri.parse("content://something/else");
    mockUri(uri);

    requestManager.asBitmap().load(uri).into(target);

    verify(target).onResourceReady(isA(Bitmap.class), isA(Transition.class));
  }

  @Test
  public void testToBytesOption() {
    Uri uri = Uri.parse("content://something/else");
    mockUri(uri);

    requestManager.as(byte[].class).apply(decodeTypeOf(Bitmap.class)).load(uri).into(target);

    verify(target).onResourceReady(isA(byte[].class), isA(Transition.class));
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

    verify(target).onResourceReady(notNull(), isA(Transition.class));
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
    requestManager.load(string).listener(new RequestListener<Drawable>() {
      @Override
      public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target,
          boolean isFirstResource) {
        throw new RuntimeException("Load failed");
      }

      @Override
      public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
          DataSource dataSource, boolean isFirstResource) {
        return false;
      }
    }).into(target);
    requestManager.load(string).into(imageView);

    verify(target).onResourceReady(isA(BitmapDrawable.class), isA(Transition.class));
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

    verify(target).onResourceReady(isA(BitmapDrawable.class), isA(Transition.class));
    verify(target).setRequest((Request) notNull());

    assertNotNull(imageView.getDrawable());
  }

  @Test
  public void testByteArrayDefaultLoader() {
    byte[] bytes = new byte[10];
    requestManager.load(bytes).into(target);
    requestManager.load(bytes).into(imageView);

    verify(target).onResourceReady(isA(BitmapDrawable.class), isA(Transition.class));
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
  public void testNonDefaultModelWithRegisteredFactoryDoesNotThrow() {
    registerMockStreamModelLoader(Float.class);

    requestManager.load(0.5f).into(target);
  }

  @Test
  public void testReceivesGif() throws IOException {
    String fakeUri = "content://fake";
    InputStream testGifData = openResource("test.gif");
    mockUri(Uri.parse(fakeUri), testGifData);

    requestManager.asGif().load(fakeUri).into(target);

    verify(target).onResourceReady(isA(GifDrawable.class), isA(Transition.class));
  }

  @Test
  public void testReceivesGifBytes() throws IOException {
    String fakeUri = "content://fake";
    InputStream testGifData = openResource("test.gif");
    mockUri(Uri.parse(fakeUri), testGifData);

    requestManager.as(byte[].class).apply(decodeTypeOf(GifDrawable.class)).load(fakeUri)
        .into(target);

    verify(target).onResourceReady(isA(byte[].class), isA(Transition.class));
  }

  @Test
  public void testReceivesBitmapBytes() {
    String fakeUri = "content://fake";
    mockUri(fakeUri);
    requestManager.as(byte[].class).apply(decodeTypeOf(Bitmap.class)).load(fakeUri).into(target);

    verify(target).onResourceReady(isA(byte[].class), isA(Transition.class));
  }

  @Test
  public void testReceivesThumbnails() {
    String full = mockUri("content://full");
    String thumb = mockUri("content://thumb");
    requestManager
        .load(full)
        .thumbnail(requestManager.load(thumb))
        .into(target);

    verify(target, times(2)).onResourceReady(isA(Drawable.class), isA(Transition.class));
  }

  @Test
  public void testReceivesRecursiveThumbnails() {
    requestManager.load(mockUri("content://first")).thumbnail(
        requestManager.load(mockUri("content://second")).thumbnail(
            requestManager.load(mockUri("content://third")).thumbnail(
                requestManager.load(mockUri("content://fourth")))))
        .into(target);
    verify(target, times(4)).onResourceReady(isA(Drawable.class), isA(Transition.class));
  }

  @Test
  public void testReceivesRecursiveThumbnailWithPercentage() {
    requestManager.load(mockUri("content://first"))
        .thumbnail(requestManager.load(mockUri("content://second")).thumbnail(0.5f))
        .into(target);
    verify(target, times(3)).onResourceReady(isA(Drawable.class), isA(Transition.class));
  }

  @Test
  public void testNullModelInGenericImageLoadDoesNotThrow() {
    requestManager.load(null).into(target);
  }

  @Test
  public void testNullModelInGenericVideoLoadDoesNotThrow() {
    requestManager.load(null).into(target);
  }

  @Test
  public void testNullModelInGenericLoadDoesNotThrow() {
    requestManager.load(null).into(target);
  }

  @Test
  public void testNullModelDoesNotThrow() {
    Drawable drawable = new ColorDrawable(Color.RED);
    requestManager
        .load(null)
        .apply(placeholderOf(drawable))
        .into(target);

    verify(target).onLoadFailed(eq(drawable));
  }

  @Test
  public void testNullModelPrefersErrorDrawable() {
    Drawable placeholder = new ColorDrawable(Color.GREEN);
    Drawable error = new ColorDrawable(Color.RED);

    requestManager
        .load(null)
        .apply(placeholderOf(placeholder)
            .error(error))
        .into(target);

    verify(target).onLoadFailed(eq(error));
  }

  @Test
  public void testNullModelPrefersFallbackDrawable() {
    Drawable placeholder = new ColorDrawable(Color.GREEN);
    Drawable error = new ColorDrawable(Color.RED);
    Drawable fallback = new ColorDrawable(Color.BLUE);

    requestManager
        .load(null)
        .apply(placeholderOf(placeholder)
            .error(error)
            .fallback(fallback))
        .into(target);

    verify(target).onLoadFailed(eq(fallback));
  }

  @Test
  public void testByteData() {
    byte[] data = new byte[] { 1, 2, 3, 4, 5, 6 };
    requestManager.load(data).into(target);
  }

  @Test
  public void testClone() throws IOException {
    Target<Drawable> firstTarget = mock(Target.class);
    doAnswer(new CallSizeReady(100, 100)).when(firstTarget).getSize(isA(SizeReadyCallback.class));
    Target<Drawable> secondTarget = mock(Target.class);
    doAnswer(new CallSizeReady(100, 100)).when(secondTarget).getSize(isA(SizeReadyCallback.class));
    RequestBuilder<Drawable> firstRequest = requestManager
        .load(mockUri("content://first"));

    firstRequest.into(firstTarget);

    firstRequest.clone()
        .apply(placeholderOf(new ColorDrawable(Color.RED)))
        .into(secondTarget);

    verify(firstTarget).onResourceReady(isA(Drawable.class), isA(Transition.class));
    verify(secondTarget).onResourceReady(notNull(Drawable.class), isA(Transition.class));
  }

  @SuppressWarnings("unchecked")
  private <T, Z> void registerFailFactory(Class<T> failModel, Class<Z> failResource)
      throws Exception {
    DataFetcher<Z> failFetcher = mock(DataFetcher.class);
    doAnswer(new Util.CallDataReady<>(null))
        .when(failFetcher)
        .loadData(isA(Priority.class), isA(DataFetcher.DataCallback.class));
    when(failFetcher.getDataClass()).thenReturn(failResource);
    ModelLoader<T, Z> failLoader = mock(ModelLoader.class);
    when(failLoader.buildLoadData(isA(failModel), anyInt(), anyInt(), isA(Options.class)))
        .thenReturn(new ModelLoader.LoadData<>(mock(Key.class), failFetcher));
    when(failLoader.handles(isA(failModel))).thenReturn(true);
    ModelLoaderFactory<T, Z> failFactory = mock(ModelLoaderFactory.class);
    when(failFactory.build(isA(MultiModelLoaderFactory.class))).thenReturn(failLoader);

    Glide.get(getContext()).getRegistry().prepend(failModel, failResource, failFactory);
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
    ContentResolver contentResolver = RuntimeEnvironment.application.getContentResolver();
    ShadowFileDescriptorContentResolver shadowContentResolver =
        (ShadowFileDescriptorContentResolver) ShadowExtractor.extract(contentResolver);
    shadowContentResolver.registerInputStream(uri, is);

    AssetFileDescriptor assetFileDescriptor = mock(AssetFileDescriptor.class);
    ParcelFileDescriptor parcelFileDescriptor = mock(ParcelFileDescriptor.class);
    when(assetFileDescriptor.getParcelFileDescriptor()).thenReturn(parcelFileDescriptor);

    shadowContentResolver.registerAssetFileDescriptor(uri, assetFileDescriptor);
    return uri.toString();
  }

  private Context getContext() {
    return RuntimeEnvironment.application;
  }

  @SuppressWarnings("unchecked")
  private <T> void registerMockStreamModelLoader(final Class<T> modelClass) {
    ModelLoader<T, InputStream> modelLoader = mockStreamModelLoader(modelClass);
    ModelLoaderFactory<T, InputStream> modelLoaderFactory = mock(ModelLoaderFactory.class);
    when(modelLoaderFactory.build(isA(MultiModelLoaderFactory.class))).thenReturn(modelLoader);

    Glide.get(RuntimeEnvironment.application).getRegistry()
        .prepend(modelClass, InputStream.class, modelLoaderFactory);
  }

  @SuppressWarnings("unchecked")
  private <T> ModelLoader<T, InputStream> mockStreamModelLoader(final Class<T> modelClass) {
    ModelLoader<T, InputStream> modelLoader = mock(ModelLoader.class);
    DataFetcher<InputStream> fetcher = mock(DataFetcher.class);
    try {
      doAnswer(new Util.CallDataReady<>(new ByteArrayInputStream(new byte[0]))).when(fetcher)
          .loadData(isA(Priority.class), isA(DataFetcher.DataCallback.class));
    } catch (Exception e) {
      // Do nothing.
    }
    when(fetcher.getDataClass()).thenReturn(InputStream.class);
    when(modelLoader.buildLoadData(isA(modelClass), anyInt(), anyInt(), isA(Options.class)))
        .thenReturn(new ModelLoader.LoadData<>(mock(Key.class), fetcher));
    when(modelLoader.handles(isA(modelClass))).thenReturn(true);

    return modelLoader;
  }

  private InputStream openResource(String imageName) throws IOException {
    return TestResourceUtil.openResource(getClass(), imageName);
  }

  private static class CallSizeReady implements Answer<Void> {
    private int width;
    private int height;

    public CallSizeReady() {
      this(100, 100);
    }

    public CallSizeReady(int width, int height) {
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

  public static class SetupModule implements GlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
      // Run all tasks on the main thread so they complete synchronously.
      GlideExecutor executor = MockGlideExecutor.newMainThreadExecutor();

      DiskCache.Factory diskCacheFactory = mock(DiskCache.Factory.class);
      when(diskCacheFactory.build()).thenReturn(mock(DiskCache.class));

      builder.setMemoryCache(mock(MemoryCache.class)).setDiskCache(diskCacheFactory)
          .setResizeExecutor(executor).setDiskCacheExecutor(executor);
    }

    @Override
    public void registerComponents(Context context, Registry registry) {
      registerMockModelLoader(GlideUrl.class, InputStream.class,
          new ByteArrayInputStream(new byte[0]), registry);
      registerMockModelLoader(File.class, InputStream.class,
          new ByteArrayInputStream(new byte[0]), registry);
      registerMockModelLoader(File.class, ParcelFileDescriptor.class,
          mock(ParcelFileDescriptor.class), registry);
      registerMockModelLoader(File.class, ByteBuffer.class,
          ByteBuffer.allocate(10), registry);
    }

    private static <X, Y> void registerMockModelLoader(Class<X> modelClass, Class<Y> dataClass,
          Y loadedData, Registry registry) {
      DataFetcher<Y> mockStreamFetcher = mock(DataFetcher.class);
      when(mockStreamFetcher.getDataClass()).thenReturn(dataClass);
      try {
        doAnswer(new Util.CallDataReady<>(loadedData))
            .when(mockStreamFetcher)
            .loadData(isA(Priority.class), isA(DataFetcher.DataCallback.class));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      ModelLoader<X, Y> mockUrlLoader = mock(ModelLoader.class);
      when(mockUrlLoader.buildLoadData(isA(modelClass), anyInt(), anyInt(), isA(Options.class)))
          .thenReturn(new ModelLoader.LoadData<>(mock(Key.class), mockStreamFetcher));
      when(mockUrlLoader.handles(isA(modelClass))).thenReturn(true);
      ModelLoaderFactory<X, Y> mockUrlLoaderFactory = mock(ModelLoaderFactory.class);
      when(mockUrlLoaderFactory.build(isA(MultiModelLoaderFactory.class)))
          .thenReturn(mockUrlLoader);

      registry.replace(modelClass, dataClass, mockUrlLoaderFactory);
    }
  }

  // TODO: Extending ShadowContentResolver results in exceptions because of some state issues
  // where we seem to get one content resolver shadow in one part of the test and a different one in
  // a different part of the test. Each one ends up with different registered uris, which causes
  // tests to fail. We shouldn't need to do this, but using static maps seems to fix the issue.
  @Implements(value = ContentResolver.class)
  @SuppressWarnings("unused")
  public static class ShadowFileDescriptorContentResolver {
    private static final Map<Uri, AssetFileDescriptor> URI_TO_FILE_DESCRIPTOR = new HashMap<>();
    private static final Map<Uri, InputStream> URI_TO_INPUT_STREAMS = new HashMap<>();

    @Resetter
    public static void reset() {
      URI_TO_INPUT_STREAMS.clear();
      URI_TO_FILE_DESCRIPTOR.clear();
    }

    public void registerInputStream(Uri uri, InputStream inputStream) {
      URI_TO_INPUT_STREAMS.put(uri, inputStream);
    }

    public void registerAssetFileDescriptor(Uri uri, AssetFileDescriptor assetFileDescriptor) {
      URI_TO_FILE_DESCRIPTOR.put(uri, assetFileDescriptor);
    }

    @Implementation
    public InputStream openInputStream(Uri uri) {
      if (!URI_TO_INPUT_STREAMS.containsKey(uri)) {
        throw new IllegalArgumentException(
            "You must first register an InputStream for uri: " + uri);
      }
      return URI_TO_INPUT_STREAMS.get(uri);
    }

    @Implementation
    public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String type) {
      if (!URI_TO_FILE_DESCRIPTOR.containsKey(uri)) {
        throw new IllegalArgumentException(
            "You must first register an AssetFileDescriptor for " + "uri: " + uri);
      }
      return URI_TO_FILE_DESCRIPTOR.get(uri);
    }
  }

  @Implements(Bitmap.class)
  public static class MutableShadowBitmap extends ShadowBitmap {

    @Implementation
    public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
      Bitmap bitmap = ShadowBitmap.createBitmap(width, height, config);
      Shadows.shadowOf(bitmap).setMutable(true);
      return bitmap;
    }
  }

  @Implements(MediaMetadataRetriever.class)
  public static class ShadowMediaMetadataRetriever {

    @Implementation
    @SuppressWarnings("unused")
    public Bitmap getFrameAtTime() {
      Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
      Shadows.shadowOf(bitmap).appendDescription(" from MediaMetadataRetriever");
      return bitmap;
    }
  }
}

