package com.bumptech.glide;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Process;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.tests.TearDownGlide;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/** Integration tests for Glide Experiments. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public final class GlideExperimentsTest {

  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();

  private Context context;
  private static final int INITIAL_CACHE_SIZE = 1000;
  private static final int INITIAL_POOL_SIZE = 1000;
  private MemoryCache memoryCache;
  private BitmapPool bitmapPool;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    memoryCache = new LruResourceCache(INITIAL_CACHE_SIZE);
    bitmapPool = new LruBitmapPool(INITIAL_POOL_SIZE);
  }

  private Glide createGlide(
      boolean enableTrimMemoryOnUiHidden, @Nullable MemoryCategory memoryCategoryInBackground) {
    return new GlideBuilder()
        .setBitmapPool(bitmapPool)
        .setMemoryCache(memoryCache)
        .setMemoryCategoryInBackground(memoryCategoryInBackground)
        .experimentalSetEnableTrimMemoryOnUiHidden(enableTrimMemoryOnUiHidden)
        .build(context, Collections.emptyList(), /* annotationGeneratedGlideModule= */ null);
  }

  @Test
  public void trimMemoryOnUiHidden_experimentEnabled_setsMemoryCategory() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ true, MemoryCategory.ZERO);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);

    assertThat(memoryCache.getMaxSize()).isEqualTo(0);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0);
  }

  @Test
  public void trimMemoryOnUiHidden_experimentDisabled_doesNotSetMemoryCategory() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ false, MemoryCategory.ZERO);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);

    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void trimMemoryBackground_experimentDisabled_setsMemoryCategory() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ false, MemoryCategory.ZERO);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);

    assertThat(memoryCache.getMaxSize()).isEqualTo(0);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0);
  }

  @Test
  public void trimMemoryBackground_experimentEnabled_setsMemoryCategory() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ true, MemoryCategory.ZERO);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);

    assertThat(memoryCache.getMaxSize()).isEqualTo(0);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0);
  }

  @Test
  public void trimMemoryRunningCritical_experimentEnabled_doesNotChangeMultiplier() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ true, MemoryCategory.ZERO);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL);

    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void trimMemoryOnUiHidden_withoutMemoryCategory_doesNotChangeMultiplier() {
    Glide glide =
        createGlide(/* enableTrimMemoryOnUiHidden= */ true, /* memoryCategoryInBackground= */ null);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);

    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void trimMemoryOnUiHidden_foregroundRestoresMemoryCategory() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ true, MemoryCategory.LOW);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
    assertThat(memoryCache.getMaxSize())
        .isEqualTo(Math.round(INITIAL_CACHE_SIZE * MemoryCategory.LOW.getMultiplier()));
    assertThat(bitmapPool.getMaxSize())
        .isEqualTo(Math.round(INITIAL_POOL_SIZE * MemoryCategory.LOW.getMultiplier()));

    glide.setMemoryCategoryWhenInForeground();
    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void respectExifOrientationInImageDecoder_experimentEnabled_isRecorded() {
    Glide.tearDown();
    GlideExecutor executor = MockGlideExecutor.newMainThreadExecutor();
    GlideBuilder builder =
        new GlideBuilder()
            .setSourceExecutor(executor)
            .setDiskCacheExecutor(executor)
            .setAnimationExecutor(executor)
            .setRespectExifOrientationInImageDecoder(true);
    Glide.init(context, builder);

    assertThat(
            Glide.get(context)
                .getGlideContext()
                .getExperiments()
                .isEnabled(GlideBuilder.RespectExifOrientationInImageDecoder.class))
        .isTrue();
  }

  @Test
  public void useBitmapFactoryForInputStreamsWithImageDecoder_experimentEnabled_isRecorded() {
    Glide.tearDown();
    GlideExecutor executor = MockGlideExecutor.newMainThreadExecutor();
    GlideBuilder builder =
        new GlideBuilder()
            .setSourceExecutor(executor)
            .setDiskCacheExecutor(executor)
            .setAnimationExecutor(executor)
            .setUseBitmapFactoryForInputStreamsWithImageDecoder(true);
    Glide.init(context, builder);

    assertThat(
            Glide.get(context)
                .getGlideContext()
                .getExperiments()
                .isEnabled(GlideBuilder.UseBitmapFactoryForInputStreamsWithImageDecoder.class))
        .isTrue();
  }

  @Test
  public void stripSensitiveHeadersOnCrossOriginRedirects_experimentEnabledByDefault() {
    Glide.tearDown();
    GlideExecutor executor = MockGlideExecutor.newMainThreadExecutor();
    GlideBuilder builder =
        new GlideBuilder()
            .setSourceExecutor(executor)
            .setDiskCacheExecutor(executor)
            .setAnimationExecutor(executor);
    Glide.init(context, builder);

    assertThat(
            Glide.get(context)
                .getGlideContext()
                .getExperiments()
                .isEnabled(GlideBuilder.StripSensitiveHeadersOnCrossOriginRedirects.class))
        .isTrue();
  }

  @Test
  public void stripSensitiveHeadersOnCrossOriginRedirects_experimentDisabled_isRecorded() {
    Glide.tearDown();
    GlideExecutor executor = MockGlideExecutor.newMainThreadExecutor();
    GlideBuilder builder =
        new GlideBuilder()
            .setSourceExecutor(executor)
            .setDiskCacheExecutor(executor)
            .setAnimationExecutor(executor)
            .experimentalSetStripSensitiveHeadersOnCrossOriginRedirects(false);
    Glide.init(context, builder);

    assertThat(
            Glide.get(context)
                .getGlideContext()
                .getExperiments()
                .isEnabled(GlideBuilder.StripSensitiveHeadersOnCrossOriginRedirects.class))
        .isFalse();
  }

  @Test
  public void
      trimMemoryOnAllActivitiesStopped_experimentEnabled_trimsCacheAndSetsBackgroundCategoryOnStop() {
    Glide.init(context, newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true));
    seedCachesWithBitmap();
    assertThat(memoryCache.getCurrentSize()).isGreaterThan(0L);
    assertThat(((LruBitmapPool) bitmapPool).getCurrentSize()).isGreaterThan(0L);
    ActivityController<Activity> controller =
        Robolectric.buildActivity(Activity.class).create().start();

    controller.stop();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isEqualTo(0L);
    assertThat(((LruBitmapPool) bitmapPool).getCurrentSize()).isEqualTo(0L);
    assertThat(memoryCache.getMaxSize()).isEqualTo(0L);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0L);
  }

  @Test
  public void trimMemoryOnAllActivitiesStopped_experimentDisabled_doesNotTrimOnStop() {
    Glide.init(context, newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ false));
    seedCachesWithBitmap();
    ActivityController<Activity> controller =
        Robolectric.buildActivity(Activity.class).create().start();

    controller.stop();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isGreaterThan(0L);
    assertThat(((LruBitmapPool) bitmapPool).getCurrentSize()).isGreaterThan(0L);
    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void trimMemoryOnAllActivitiesStopped_configurationChange_doesNotTrimOnStop() {
    Glide.init(context, newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true));
    seedCachesWithBitmap();
    ActivityController<ConfigChangingActivity> controller =
        Robolectric.buildActivity(ConfigChangingActivity.class).create().start();

    controller.stop();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isGreaterThan(0L);
    assertThat(((LruBitmapPool) bitmapPool).getCurrentSize()).isGreaterThan(0L);
    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void
      trimMemoryOnAllActivitiesStopped_multipleActivities_trimsOnlyAfterLastActivityStops() {
    Glide.init(context, newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true));
    seedCachesWithBitmap();
    ActivityController<Activity> first = Robolectric.buildActivity(Activity.class).create().start();
    ActivityController<Activity> second =
        Robolectric.buildActivity(Activity.class).create().start();

    first.stop();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isGreaterThan(0L);
    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);

    second.stop();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isEqualTo(0L);
    assertThat(memoryCache.getMaxSize()).isEqualTo(0L);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0L);
  }

  @Test
  public void
      trimMemoryOnAllActivitiesStopped_lazyInitAfterActivityStarted_trimsWhenActivityStops() {
    ActivityController<Activity> controller =
        Robolectric.buildActivity(Activity.class).create().start().resume();
    Glide.init(context, newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true));
    seedCachesWithBitmap();

    controller.pause().stop();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isEqualTo(0L);
    assertThat(memoryCache.getMaxSize()).isEqualTo(0L);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0L);
  }

  @Test
  public void
      trimMemoryOnAllActivitiesStopped_coldStartForegroundService_startsTrimmedAndRestoresOnStart() {
    ActivityManager activityManager =
        (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.RunningAppProcessInfo processInfo = new ActivityManager.RunningAppProcessInfo();
    processInfo.pid = Process.myPid();
    processInfo.importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE;
    Shadows.shadowOf(activityManager).setProcesses(Collections.singletonList(processInfo));

    Glide.init(context, newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true));

    assertThat(memoryCache.getMaxSize()).isEqualTo(0L);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0L);

    Robolectric.buildActivity(Activity.class).create().start();

    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void
      trimMemoryOnAllActivitiesStopped_backgroundThreadColdStartFgs_doesNotCrashAndSetsBackgroundCategory()
          throws Exception {
    ActivityManager activityManager =
        (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.RunningAppProcessInfo processInfo = new ActivityManager.RunningAppProcessInfo();
    processInfo.pid = Process.myPid();
    processInfo.importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE;
    Shadows.shadowOf(activityManager).setProcesses(Collections.singletonList(processInfo));
    GlideBuilder builder = newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true);
    AtomicReference<Throwable> initFailure = new AtomicReference<>();
    Thread initThread =
        new Thread(() -> {
          try {
            Glide.init(context, builder);
          } catch (Throwable t) {
            initFailure.set(t);
          }
        });

    initThread.start();
    initThread.join();
    if (initFailure.get() != null) {
      throw new AssertionError("Glide.init failed on background thread", initFailure.get());
    }
    ShadowLooper.idleMainLooper();

    assertThat(Glide.isInitialized()).isTrue();
    assertThat(memoryCache.getMaxSize()).isEqualTo(0L);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0L);
  }

  @Test
  public void
      trimMemoryOnAllActivitiesStopped_backgroundThreadColdStartFgsThenActivityStarts_cancelsInitialTrim()
          throws Exception {
    ActivityManager activityManager =
        (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.RunningAppProcessInfo processInfo = new ActivityManager.RunningAppProcessInfo();
    processInfo.pid = Process.myPid();
    processInfo.importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE;
    Shadows.shadowOf(activityManager).setProcesses(Collections.singletonList(processInfo));
    GlideBuilder builder = newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true);
    AtomicReference<Throwable> initFailure = new AtomicReference<>();
    Thread initThread =
        new Thread(() -> {
          try {
            Glide.init(context, builder);
          } catch (Throwable t) {
            initFailure.set(t);
          }
        });

    initThread.start();
    initThread.join();
    if (initFailure.get() != null) {
      throw new AssertionError("Glide.init failed on background thread", initFailure.get());
    }
    Robolectric.buildActivity(Activity.class).create().start();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void trimMemoryOnAllActivitiesStopped_immediateActivityTransition_doesNotTrimCaches() {
    Glide.init(context, newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true));
    seedCachesWithBitmap();
    ActivityController<Activity> first = Robolectric.buildActivity(Activity.class).create().start();

    first.stop();
    Robolectric.buildActivity(Activity.class).create().start();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isGreaterThan(0L);
    assertThat(((LruBitmapPool) bitmapPool).getCurrentSize()).isGreaterThan(0L);
    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void
      trimMemoryOnAllActivitiesStopped_lazyInitWithStartedFragmentActivity_doesNotTrimUntilFragmentActivityStops() {
    ActivityController<FragmentActivity> startedFragmentActivity =
        Robolectric.buildActivity(FragmentActivity.class).create().start();
    Glide.init(context, newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true));
    Glide.with(startedFragmentActivity.get());
    seedCachesWithBitmap();
    ActivityController<Activity> transientActivity =
        Robolectric.buildActivity(Activity.class).create().start();

    transientActivity.stop();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isGreaterThan(0L);
    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);

    startedFragmentActivity.stop();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isEqualTo(0L);
    assertThat(memoryCache.getMaxSize()).isEqualTo(0L);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0L);
  }

  @Test
  public void
      trimMemoryOnAllActivitiesStopped_fragmentBoundViaGlideWith_trimsWhenHostActivityStops() {
    ActivityController<FragmentActivity> hostActivity =
        Robolectric.buildActivity(FragmentActivity.class).create().start();
    Fragment fragment = new Fragment();
    hostActivity
        .get()
        .getSupportFragmentManager()
        .beginTransaction()
        .add(fragment, "tag")
        .commitNow();
    Glide.init(context, newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true));
    Glide.with(fragment);
    seedCachesWithBitmap();

    hostActivity.stop();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isEqualTo(0L);
    assertThat(((LruBitmapPool) bitmapPool).getCurrentSize()).isEqualTo(0L);
    assertThat(memoryCache.getMaxSize()).isEqualTo(0L);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0L);
  }

  @Test
  public void
      trimMemoryOnAllActivitiesStopped_withoutBackgroundMemoryCategory_trimsCachesOnUiHidden() {
    GlideBuilder builder =
        newTrimOnAllActivitiesStoppedBuilder(/* isEnabled= */ true)
            .setMemoryCategoryInBackground(/* memoryCategory= */ null);
    Glide.init(context, builder);
    seedCachesWithBitmap();
    memoryCache.put(
        new ObjectKey("cached_second"),
        BitmapResource.obtain(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888), bitmapPool));
    long seededCacheSize = memoryCache.getCurrentSize();
    assertThat(seededCacheSize).isGreaterThan(INITIAL_CACHE_SIZE / 2L);
    ActivityController<Activity> controller =
        Robolectric.buildActivity(Activity.class).create().start();

    controller.stop();
    ShadowLooper.idleMainLooper();

    assertThat(memoryCache.getCurrentSize()).isAtMost(INITIAL_CACHE_SIZE / 2L);
    assertThat(memoryCache.getCurrentSize()).isLessThan(seededCacheSize);
    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  private GlideBuilder newTrimOnAllActivitiesStoppedBuilder(boolean isEnabled) {
    GlideExecutor executor = MockGlideExecutor.newMainThreadExecutor();
    return new GlideBuilder()
        .setBitmapPool(bitmapPool)
        .setMemoryCache(memoryCache)
        .setSourceExecutor(executor)
        .setDiskCacheExecutor(executor)
        .setAnimationExecutor(executor)
        .setMemoryCategoryInBackground(MemoryCategory.ZERO)
        .experimentalSetTrimMemoryOnAllActivitiesStopped(isEnabled);
  }

  private void seedCachesWithBitmap() {
    Bitmap cachedBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    memoryCache.put(new ObjectKey("cached"), BitmapResource.obtain(cachedBitmap, bitmapPool));
    bitmapPool.put(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888));
  }

  private static final class ConfigChangingActivity extends Activity {
    @Override
    public boolean isChangingConfigurations() {
      return true;
    }
  }
}
