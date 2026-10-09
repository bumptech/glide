package com.bumptech.glide.load.engine;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import android.util.Log;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.GlideBuilder.EnableActiveResourceReleaseRaceFix;
import com.bumptech.glide.GlideContext;
import com.bumptech.glide.GlideExperiments;
import com.bumptech.glide.GlideExperimentsTestUtil;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.signature.EmptySignature;
import com.bumptech.glide.util.Executors;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.util.concurrent.PausedExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
@LooperMode(LooperMode.Mode.PAUSED)
public class EngineJobReleaseRaceTest {
  private static final long TIMEOUT_SECONDS = 10;
  private static final Object MODEL = "model";
  private static final int SIZE = 100;
  private static final GlideExperiments WITH_FIX =
      GlideExperimentsTestUtil.withExperiment(new EnableActiveResourceReleaseRaceFix());
  private static final GlideExperiments WITHOUT_FIX = GlideExperimentsTestUtil.empty();

  private final Map<Class<?>, Transformation<?>> transformations = new HashMap<>();
  private final Options options = new Options();
  private final EngineKey key =
      new EngineKey(
          MODEL,
          EmptySignature.obtain(),
          SIZE,
          SIZE,
          transformations,
          /* resourceClass= */ Object.class,
          /* transcodeClass= */ Object.class,
          options);
  private final ActiveResources activeResources =
      new ActiveResources(
          /* isActiveResourceRetentionAllowed= */ false,
          /* monitorClearedResourcesExecutor= */ unused -> {});
  private final LruResourceCache memoryCache = new LruResourceCache(/* size= */ 100);

  /** Holds the decode a new load starts; the test never runs it. */
  private final PausedExecutorService jobService = new PausedExecutorService();

  private Engine engine;

  @Before
  public void setUp() {
    GlideExecutor jobExecutor = MockGlideExecutor.newTestExecutor(jobService);
    engine =
        new Engine(
            memoryCache,
            new DiskCacheAdapter.Factory(),
            /* diskCacheExecutor= */ jobExecutor,
            /* sourceExecutor= */ jobExecutor,
            /* sourceUnlimitedExecutor= */ jobExecutor,
            /* animationExecutor= */ jobExecutor,
            /* jobs= */ null,
            /* keyFactory= */ null,
            activeResources,
            /* engineJobFactory= */ null,
            /* decodeJobFactory= */ null,
            /* resourceRecycler= */ null,
            /* isActiveResourceRetentionAllowed= */ false);
  }

  @After
  public void tearDown() {
    jobService.shutdownNow();
  }

  @Test
  public void loadWhileLastHoldIsReleasedOnAnotherThread_withFix_evictionDoesNotThrow()
      throws Exception {
    RecordingCallback request = loadWhileLastHoldIsReleasedOnAnotherThread(WITH_FIX);

    assertThat(request.resource).isNull();
    assertThat(jobService.hasQueuedTasks()).isTrue();
    assertThat(memoryCache.contains(key)).isTrue();
    memoryCache.clearMemory();
    shadowOf(Looper.getMainLooper()).idle();
  }

  @Test
  public void loadWhileLastHoldIsReleasedOnAnotherThread_withoutFix_evictionThrows()
      throws Exception {
    RecordingCallback request = loadWhileLastHoldIsReleasedOnAnotherThread(WITHOUT_FIX);

    assertThat(request.resource).isSameInstanceAs(memoryCache.get(key));
    memoryCache.clearMemory();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> shadowOf(Looper.getMainLooper()).idle());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo("Cannot recycle a resource while it is still acquired");
  }

  /**
   * Loads {@link #MODEL} while another thread releases the last hold on its active resource: after
   * the count drops to 0, but before the Engine moves the resource to the memory cache.
   */
  private RecordingCallback loadWhileLastHoldIsReleasedOnAnotherThread(GlideExperiments experiments)
      throws Exception {
    CountDownLatch countDroppedToZero = new CountDownLatch(1);
    CountDownLatch resumeRelease = new CountDownLatch(1);
    EngineResource<?> resource = new PausingEngineResource(countDroppedToZero, resumeRelease);
    resource.acquire();
    activeResources.activate(key, resource);
    FutureTask<Void> release = runOnAnotherThread(resource::release);
    await(countDroppedToZero);

    RecordingCallback request = new RecordingCallback();
    load(newGlideContext(experiments), MODEL, request);
    resumeRelease.countDown();
    release.get(TIMEOUT_SECONDS, SECONDS);
    return request;
  }

  private static FutureTask<Void> runOnAnotherThread(Runnable runnable) {
    FutureTask<Void> task = new FutureTask<>(runnable, /* result= */ null);
    new Thread(task).start();
    return task;
  }

  private GlideContext newGlideContext(GlideExperiments experiments) {
    return new GlideContext(
        ApplicationProvider.getApplicationContext(),
        new LruArrayPool(),
        Registry::new,
        new ImageViewTargetFactory(),
        RequestOptions::new,
        /* defaultTransitionOptions= */ Collections.emptyMap(),
        /* defaultRequestListeners= */ Collections.emptyList(),
        engine,
        experiments,
        Log.DEBUG);
  }

  private void load(GlideContext glideContext, Object model, ResourceCallback cb) {
    engine.load(
        glideContext,
        model,
        EmptySignature.obtain(),
        SIZE,
        SIZE,
        /* resourceClass= */ Object.class,
        /* transcodeClass= */ Object.class,
        Priority.NORMAL,
        DiskCacheStrategy.NONE,
        transformations,
        /* isTransformationRequired= */ false,
        /* isScaleOnlyOrNoTransform= */ true,
        options,
        /* isMemoryCacheable= */ true,
        /* useUnlimitedSourceExecutorPool= */ false,
        /* useAnimationPool= */ false,
        /* onlyRetrieveFromCache= */ false,
        cb,
        Executors.directExecutor());
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(TIMEOUT_SECONDS, SECONDS)) {
        throw new AssertionError("Timed out waiting for the latch");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }
  }

  /**
   * When its last hold is dropped, counts down {@code countDroppedToZero}, then waits for {@code
   * resumeRelease} before the Engine removes it from active resources.
   */
  private final class PausingEngineResource extends EngineResource<Object> {
    PausingEngineResource(CountDownLatch countDroppedToZero, CountDownLatch resumeRelease) {
      super(
          new FakeResource(),
          /* isMemoryCacheable= */ true,
          /* isRecyclable= */ true,
          key,
          // Normally the Engine itself is the listener.
          (releasedKey, releasedResource) -> {
            countDroppedToZero.countDown();
            await(resumeRelease);
            engine.onResourceReleased(releasedKey, releasedResource);
          });
    }
  }

  /** Minimal stand-in for a Request: records the resource it was given. */
  private static final class RecordingCallback implements ResourceCallback {
    private final Object lock = new Object();
    volatile Resource<?> resource;

    @Override
    public void onResourceReady(
        Resource<?> resource, DataSource dataSource, boolean isLoadedFromAlternateCacheKey) {
      this.resource = resource;
    }

    @Override
    public void onLoadFailed(GlideException e) {
      throw new AssertionError(e);
    }

    @Override
    public Object getLock() {
      return lock;
    }
  }

  private static final class FakeResource implements Resource<Object> {
    private final Object value = new Object();

    @Override
    public Class<Object> getResourceClass() {
      return Object.class;
    }

    @Override
    public Object get() {
      return value;
    }

    @Override
    public int getSize() {
      return 1;
    }

    @Override
    public void recycle() {}
  }
}
