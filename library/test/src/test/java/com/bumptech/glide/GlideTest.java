package com.bumptech.glide;

import static com.bumptech.glide.request.RequestOptions.decodeTypeOf;
import static com.bumptech.glide.request.RequestOptions.errorOf;
import static com.bumptech.glide.request.RequestOptions.placeholderOf;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
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
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.tests.GlideShadowLooper;
import com.bumptech.glide.tests.TearDownGlide;
import com.bumptech.glide.tests.Util;
import com.bumptech.glide.testutil.TestResourceUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.LooperMode;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowBitmap;

/** Tests for the {@link Glide} interface and singleton. */
@LooperMode(LEGACY)
@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 18,
    shadows = {
      GlideTest.ShadowFileDescriptorContentResolver.class,
      GlideTest.ShadowMediaMetadataRetriever.class,
      GlideShadowLooper.class,
      GlideTest.MutableShadowBitmap.class
    })
@SuppressWarnings("unchecked")
public class GlideTest {
  // Fixes method overload confusion.
  private static final Object NULL = null;

  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();

  @SuppressWarnings("rawtypes")
  @Mock
  private Target target;

  @Mock private DiskCache.Factory diskCacheFactory;
  @Mock private DiskCache diskCache;
  @Mock private MemoryCache memoryCache;
  @Mock private Handler bgHandler;
  @Mock private Lifecycle lifecycle;
  @Mock private RequestManagerTreeNode treeNode;
  @Mock private BitmapPool bitmapPool;

  private ImageView imageView;
  private RequestManager requestManager;
  private Context context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();

    // Run all tasks on the main thread so they complete synchronously.
    GlideExecutor executor = MockGlideExecutor.newMainThreadExecutor();
    when(diskCacheFactory.build()).thenReturn(diskCache);
    Glide.init(
        context,
        new GlideBuilder()
            .setMemoryCache(memoryCache)
            .setDiskCache(diskCacheFactory)
            .setSourceExecutor(executor)
            .setDiskCacheExecutor(executor));
    Registry registry = Glide.get(context).getRegistry();
    registerMockModelLoader(
        GlideUrl.class, InputStream.class, new ByteArrayInputStream(new byte[0]), registry);
    registerMockModelLoader(
        File.class, InputStream.class, new ByteArrayInputStream(new byte[0]), registry);
    registerMockModelLoader(
        File.class, ParcelFileDescriptor.class, mock(ParcelFileDescriptor.class), registry);
    registerMockModelLoader(File.class, ByteBuffer.class, ByteBuffer.allocate(10), registry);

    // Ensure that target's size ready callback will be called synchronously.
    imageView = new ImageView(context);
    imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
    imageView.layout(0, 0, 100, 100);
    doAnswer(new CallSizeReady()).when(target).getSize(isA(SizeReadyCallback.class));

    when(bgHandler.post(isA(Runnable.class)))
        .thenAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return true;
              }
            });

    requestManager = new RequestManager(Glide.get(context), lifecycle, treeNode, context);
    requestManager.resumeRequests();
  }

  @Test
  public void testCanSetMemoryCategory() {
    MemoryCategory memoryCategory = MemoryCategory.NORMAL;
    Glide glide =
        new GlideBuilder().setBitmapPool(bitmapPool).setMemoryCache(memoryCache).build(context);
    glide.setMemoryCategory(memoryCategory);

    verify(memoryCache).setSizeMultiplier(eq(memoryCategory.getMultiplier()));
    verify(bitmapPool).setSizeMultiplier(eq(memoryCategory.getMultiplier()));
  }

  @Test
  public void testCanIncreaseMemoryCategory() {
    MemoryCategory memoryCategory = MemoryCategory.NORMAL;
    Glide glide =
        new GlideBuilder().setBitmapPool(bitmapPool).setMemoryCache(memoryCache).build(context);
    glide.setMemoryCategory(memoryCategory);

    verify(memoryCache).setSizeMultiplier(eq(memoryCategory.getMultiplier()));
    verify(bitmapPool).setSizeMultiplier(eq(memoryCategory.getMultiplier()));

    MemoryCategory newMemoryCategory = MemoryCategory.HIGH;
    MemoryCategory oldMemoryCategory = glide.setMemoryCategory(newMemoryCategory);

    assertEquals(memoryCategory, oldMemoryCategory);

    verify(memoryCache).setSizeMultiplier(eq(newMemoryCategory.getMultiplier()));
    verify(bitmapPool).setSizeMultiplier(eq(newMemoryCategory.getMultiplier()));
  }

  @Test
  public void testCanDecreaseMemoryCategory() {
    MemoryCategory memoryCategory = MemoryCategory.NORMAL;
    Glide glide =
        new GlideBuilder().setBitmapPool(bitmapPool).setMemoryCache(memoryCache).build(context);
    glide.setMemoryCategory(memoryCategory);

    verify(memoryCache).setSizeMultiplier(eq(memoryCategory.getMultiplier()));
    verify(bitmapPool).setSizeMultiplier(eq(memoryCategory.getMultiplier()));

    MemoryCategory newMemoryCategory = MemoryCategory.LOW;
    MemoryCategory oldMemoryCategory = glide.setMemoryCategory(newMemoryCategory);

    assertEquals(memoryCategory, oldMemoryCategory);

    verify(memoryCache).setSizeMultiplier(eq(newMemoryCategory.getMultiplier()));
    verify(bitmapPool).setSizeMultiplier(eq(newMemoryCategory.getMultiplier()));
  }

  @Test
  public void testClearMemory() {
    Glide glide =
        new GlideBuilder().setBitmapPool(bitmapPool).setMemoryCache(memoryCache).build(context);

    glide.clearMemory();

    verify(bitmapPool).clearMemory();
    verify(memoryCache).clearMemory();
  }

  @Test
  public void testTrimMemory() {
    Glide glide =
        new GlideBuilder().setBitmapPool(bitmapPool).setMemoryCache(memoryCache).build(context);

    final int level = 123;

    glide.trimMemory(level);

    verify(bitmapPool).trimMemory(eq(level));
    verify(memoryCache).trimMemory(eq(level));
  }

  @Test
  public void testFileDefaultLoaderWithInputStream() {
    registerFailFactory(File.class, ParcelFileDescriptor.class);
    runTestFileDefaultLoader();
  }

  @Test
  public void testFileDefaultLoaderWithFileDescriptor() {
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

  @SuppressWarnings("deprecation")
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
  public void testLoadColorDrawable_withUnitBitmapTransformation_returnsColorDrawable() {
    ColorDrawable colorDrawable = new ColorDrawable(Color.RED);
    requestManager
        .load(colorDrawable)
        .apply(new RequestOptions().override(100, 100).centerCrop())
        .into(target);

    ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
    verify(target).onResourceReady(argumentCaptor.capture(), isA(Transition.class));

    Object result = argumentCaptor.getValue();

    assertThat(result).isInstanceOf(ColorDrawable.class);
    assertThat(((ColorDrawable) result).getColor()).isEqualTo(Color.RED);
  }

  @Test
  public void testLoadColorDrawable_withNonUnitBitmapTransformation_returnsBitmapDrawable() {
    ColorDrawable colorDrawable = new ColorDrawable(Color.RED);
    requestManager
        .load(colorDrawable)
        .apply(new RequestOptions().override(100, 100).circleCrop())
        .into(target);

    ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
    verify(target).onResourceReady(argumentCaptor.capture(), isA(Transition.class));

    Object result = argumentCaptor.getValue();

    assertThat(result).isInstanceOf(BitmapDrawable.class);
    Bitmap bitmap = ((BitmapDrawable) result).getBitmap();
    assertThat(bitmap.getWidth()).isEqualTo(100);
    assertThat(bitmap.getHeight()).isEqualTo(100);
  }

  @Test
  public void testUriDefaultLoaderWithInputStream() {
    registerFailFactory(Uri.class, ParcelFileDescriptor.class);
    runTestUriDefaultLoader();
  }

  @Test
  public void testUriDefaultLoaderWithFileDescriptor() {
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
  public void testFileStringDefaultLoaderWithInputStream() {
    registerFailFactory(String.class, ParcelFileDescriptor.class);
    runTestFileStringDefaultLoader();
  }

  @Test
  public void testFileStringDefaultLoaderWithFileDescriptor() {
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
  public void testUriStringDefaultLoaderWithInputStream() {
    registerFailFactory(String.class, ParcelFileDescriptor.class);
    runTestUriStringDefaultLoader();
  }

  @Test
  public void testUriStringDefaultLoaderWithFileDescriptor() {
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
        .listener(
            new RequestListener<Drawable>() {
              @Override
              public boolean onLoadFailed(
                  GlideException e,
                  Object model,
                  Target<Drawable> target,
                  boolean isFirstResource) {
                throw new RuntimeException("Load failed");
              }

              @Override
              public boolean onResourceReady(
                  Drawable resource,
                  Object model,
                  Target<Drawable> target,
                  DataSource dataSource,
                  boolean isFirstResource) {
                return false;
              }
            })
        .into(target);
    requestManager.load(string).into(imageView);

    verify(target).onResourceReady(isA(BitmapDrawable.class), isA(Transition.class));
    verify(target).setRequest((Request) notNull());

    assertNotNull(imageView.getDrawable());
  }

  @Test
  public void testIntegerDefaultLoaderWithInputStream() {
    registerFailFactory(Integer.class, ParcelFileDescriptor.class);
    runTestIntegerDefaultLoader();
  }

  @Test
  public void testIntegerDefaultLoaderWithFileDescriptor() {
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
  public void testReceivesGif() {
    String fakeUri = "content://fake";
    InputStream testGifData = openGif();
    mockUri(Uri.parse(fakeUri), testGifData);

    requestManager.asGif().load(fakeUri).into(target);

    verify(target).onResourceReady(isA(GifDrawable.class), isA(Transition.class));
  }

  @Test
  public void testReceivesGifBytes() {
    String fakeUri = "content://fake";
    InputStream testGifData = openGif();
    mockUri(Uri.parse(fakeUri), testGifData);

    requestManager
        .as(byte[].class)
        .apply(decodeTypeOf(GifDrawable.class))
        .load(fakeUri)
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
    requestManager.load(full).thumbnail(requestManager.load(thumb)).into(target);

    verify(target, times(2)).onResourceReady(isA(Drawable.class), isA(Transition.class));
  }

  @Test
  public void testReceivesRecursiveThumbnails() {
    requestManager
        .load(mockUri("content://first"))
        .thumbnail(
            requestManager
                .load(mockUri("content://second"))
                .thumbnail(
                    requestManager
                        .load(mockUri("content://third"))
                        .thumbnail(requestManager.load(mockUri("content://fourth")))))
        .into(target);
    verify(target, times(4)).onResourceReady(isA(Drawable.class), isA(Transition.class));
  }

  @Test
  public void testReceivesRecursiveThumbnailWithPercentage() {
    requestManager
        .load(mockUri("content://first"))
        .thumbnail(requestManager.load(mockUri("content://second")).thumbnail(0.5f))
        .into(target);
    verify(target, times(3)).onResourceReady(isA(Drawable.class), isA(Transition.class));
  }

  @Test
  public void testNullModelInGenericImageLoadDoesNotThrow() {
    requestManager.load(NULL).into(target);
  }

  @Test
  public void testNullModelInGenericVideoLoadDoesNotThrow() {
    requestManager.load(NULL).into(target);
  }

  @Test
  public void testNullModelInGenericLoadDoesNotThrow() {
    requestManager.load(NULL).into(target);
  }

  @Test
  public void testNullModelDoesNotThrow() {
    Drawable drawable = new ColorDrawable(Color.RED);
    requestManager.load(NULL).apply(errorOf(drawable)).into(target);

    verify(target).onLoadFailed(eq(drawable));
  }

  @Test
  public void testNullModelPrefersErrorDrawable() {
    Drawable placeholder = new ColorDrawable(Color.GREEN);
    Drawable error = new ColorDrawable(Color.RED);

    requestManager.load(NULL).apply(placeholderOf(placeholder).error(error)).into(target);

    verify(target).onLoadFailed(eq(error));
  }

  @Test
  public void testLoadBitmap_asBitmap() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    requestManager.asBitmap().load(bitmap).into(target);

    verify(target).onResourceReady(eq(bitmap), any(Transition.class));
  }

  @Test
  public void testLoadBitmap_asDrawable() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    requestManager.load(bitmap).into(target);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(target).onResourceReady(captor.capture(), any(Transition.class));
    BitmapDrawable drawable = (BitmapDrawable) captor.getValue();
    assertThat(drawable.getBitmap()).isEqualTo(bitmap);
  }

  @Test
  public void testLoadDrawable() {
    Drawable drawable = new ColorDrawable(Color.RED);
    requestManager.load(drawable).into(target);

    ArgumentCaptor<Drawable> drawableCaptor = ArgumentCaptor.forClass(Drawable.class);
    verify(target).onResourceReady(drawableCaptor.capture(), any(Transition.class));
    assertThat(((ColorDrawable) drawableCaptor.getValue()).getColor()).isEqualTo(Color.RED);
  }

  @Test
  public void testNullModelPrefersFallbackDrawable() {
    Drawable placeholder = new ColorDrawable(Color.GREEN);
    Drawable error = new ColorDrawable(Color.RED);
    Drawable fallback = new ColorDrawable(Color.BLUE);

    requestManager
        .load(NULL)
        .apply(placeholderOf(placeholder).error(error).fallback(fallback))
        .into(target);

    verify(target).onLoadFailed(eq(fallback));
  }

  @Test
  public void testNullModelResolvesToUsePlaceholder() {
    Drawable placeholder = new ColorDrawable(Color.GREEN);

    requestManager.load(NULL).apply(placeholderOf(placeholder)).into(target);

    verify(target).onLoadFailed(eq(placeholder));
  }

  @Test
  public void testByteData() {
    byte[] data = new byte[] {1, 2, 3, 4, 5, 6};
    requestManager.load(data).into(target);
  }

  @Test
  public void removeFromManagers_afterRequestManagerRemoved_clearsRequest() {
    target =
        requestManager
            .load(mockUri("content://uri"))
            .into(
                new CustomTarget<Drawable>() {
                  @Override
                  public void onResourceReady(
                      @NonNull Drawable resource,
                      @Nullable Transition<? super Drawable> transition) {
                    // Do nothing.
                  }

                  @Override
                  public void onLoadCleared(@Nullable Drawable placeholder) {
                    // Do nothing, we don't retain a reference to our resource.
                  }
                });

    requestManager.onDestroy();
    requestManager.clear(target);

    assertThat(target.getRequest()).isNull();
  }

  @Test
  public void testClone() {
    Target<Drawable> firstTarget = mock(Target.class);
    doAnswer(new CallSizeReady(100, 100)).when(firstTarget).getSize(isA(SizeReadyCallback.class));
    Target<Drawable> secondTarget = mock(Target.class);
    doAnswer(new CallSizeReady(100, 100)).when(secondTarget).getSize(isA(SizeReadyCallback.class));
    RequestBuilder<Drawable> firstRequest = requestManager.load(mockUri("content://first"));

    firstRequest.into(firstTarget);

    firstRequest.clone().apply(placeholderOf(new ColorDrawable(Color.RED))).into(secondTarget);

    verify(firstTarget).onResourceReady(isA(Drawable.class), isA(Transition.class));
    verify(secondTarget).onResourceReady(notNull(Drawable.class), isA(Transition.class));
  }

  @SuppressWarnings("unchecked")
  private <T, Z> void registerFailFactory(Class<T> failModel, Class<Z> failResource) {
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

    Glide.get(context).getRegistry().prepend(failModel, failResource, failFactory);
  }

  private String mockUri(String uriString) {
    return mockUri(Uri.parse(uriString), null);
  }

  private void mockUri(Uri uri) {
    mockUri(uri, null);
  }

  private String mockUri(Uri uri, InputStream is) {
    if (is == null) {
      is = new ByteArrayInputStream(new byte[0]);
    }
    ContentResolver contentResolver = context.getContentResolver();
    ShadowFileDescriptorContentResolver shadowContentResolver = Shadow.extract(contentResolver);
    shadowContentResolver.registerInputStream(uri, is);

    AssetFileDescriptor assetFileDescriptor = mock(AssetFileDescriptor.class);
    ParcelFileDescriptor parcelFileDescriptor = mock(ParcelFileDescriptor.class);
    when(assetFileDescriptor.getParcelFileDescriptor()).thenReturn(parcelFileDescriptor);

    shadowContentResolver.registerAssetFileDescriptor(uri, assetFileDescriptor);
    return uri.toString();
  }

  @SuppressWarnings("unchecked")
  private <T> void registerMockStreamModelLoader(final Class<T> modelClass) {
    ModelLoader<T, InputStream> modelLoader = mockStreamModelLoader(modelClass);
    ModelLoaderFactory<T, InputStream> modelLoaderFactory = mock(ModelLoaderFactory.class);
    when(modelLoaderFactory.build(isA(MultiModelLoaderFactory.class))).thenReturn(modelLoader);

    Glide.get(context).getRegistry().prepend(modelClass, InputStream.class, modelLoaderFactory);
  }

  @SuppressWarnings("unchecked")
  private <T> ModelLoader<T, InputStream> mockStreamModelLoader(final Class<T> modelClass) {
    ModelLoader<T, InputStream> modelLoader = mock(ModelLoader.class);
    DataFetcher<InputStream> fetcher = mock(DataFetcher.class);
    try {
      doAnswer(new Util.CallDataReady<>(new ByteArrayInputStream(new byte[0])))
          .when(fetcher)
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

  private InputStream openGif() {
    return TestResourceUtil.openResource(getClass(), "test.gif");
  }

  private static class CallSizeReady implements Answer<Void> {
    private final int width;
    private final int height;

    CallSizeReady() {
      this(100, 100);
    }

    CallSizeReady(int width, int height) {
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

  private static <X, Y> void registerMockModelLoader(
      Class<X> modelClass, Class<Y> dataClass, Y loadedData, Registry registry) {
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
    when(mockUrlLoaderFactory.build(isA(MultiModelLoaderFactory.class))).thenReturn(mockUrlLoader);

    registry.replace(modelClass, dataClass, mockUrlLoaderFactory);
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

    void registerInputStream(Uri uri, InputStream inputStream) {
      URI_TO_INPUT_STREAMS.put(uri, inputStream);
    }

    void registerAssetFileDescriptor(Uri uri, AssetFileDescriptor assetFileDescriptor) {
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
