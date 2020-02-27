package com.bumptech.glide.load.engine;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.os.Looper;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.ActiveResources.DequeuedResourceCallback;
import com.bumptech.glide.load.engine.ActiveResources.ResourceWeakReference;
import com.bumptech.glide.load.engine.EngineResource.ResourceListener;
import com.bumptech.glide.tests.GlideShadowLooper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@LooperMode(LEGACY)
@RunWith(RobolectricTestRunner.class)
@Config(shadows = GlideShadowLooper.class)
public class ActiveResourcesTest {

  @Mock private ResourceListener listener;
  @Mock private Key key;
  @Mock private Resource<Object> resource;

  private ActiveResources resources;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    resources = new ActiveResources(/*isActiveResourceRetentionAllowed=*/ true);
    resources.setListener(listener);

    reset(GlideShadowLooper.queue);
  }

  @After
  public void tearDown() {
    resources.shutdown();
  }

  @Test
  public void get_withMissingKey_returnsNull() {
    assertThat(resources.get(key)).isNull();
  }

  @Test
  public void get_withActiveKey_returnsResource() {
    EngineResource<Object> expected = newCacheableEngineResource();
    resources.activate(key, expected);
    assertThat(resources.get(key)).isEqualTo(expected);
  }

  @Test
  public void get_withDeactivatedKey_returnsNull() {
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);
    resources.deactivate(key);
    assertThat(resources.get(key)).isNull();
  }

  @Test
  public void deactivate_withNotActiveKey_doesNotThrow() {
    resources.deactivate(key);
  }

  @Test
  public void get_withActiveAndClearedKey_returnsNull() {
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);
    resources.activeEngineResources.get(key).clear();
    assertThat(resources.get(key)).isNull();
  }

  @Test
  public void get_withActiveAndClearedKey_andCacheableResource_callsListenerWithWrappedResource() {
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);
    resources.activeEngineResources.get(key).clear();
    resources.get(key);

    ArgumentCaptor<EngineResource<?>> captor = getEngineResourceCaptor();

    verify(listener).onResourceReleased(eq(key), captor.capture());

    assertThat(captor.getValue().getResource()).isEqualTo(resource);
  }

  @Test
  public void get_withActiveAndClearedKey_andCacheableResource_callsListenerWithNotRecycleable() {
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);
    resources.activeEngineResources.get(key).clear();
    resources.get(key);

    ArgumentCaptor<EngineResource<?>> captor = getEngineResourceCaptor();

    verify(listener).onResourceReleased(eq(key), captor.capture());

    captor.getValue().recycle();
    verify(resource, never()).recycle();
  }

  @Test
  public void get_withActiveAndClearedKey_andCacheableResource_callsListenerWithCacheable() {
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);
    resources.activeEngineResources.get(key).clear();
    resources.get(key);

    ArgumentCaptor<EngineResource<?>> captor = getEngineResourceCaptor();

    verify(listener).onResourceReleased(eq(key), captor.capture());

    assertThat(captor.getValue().isMemoryCacheable()).isTrue();
  }

  @Test
  public void get_withActiveAndClearedKey_andNotCacheableResource_doesNotCallListener() {
    EngineResource<Object> engineResource = newNonCacheableEngineResource();
    resources.activate(key, engineResource);
    resources.activeEngineResources.get(key).clear();
    resources.get(key);

    verify(listener, never()).onResourceReleased(any(Key.class), any(EngineResource.class));
  }

  @Test
  public void queueIdle_afterResourceRemovedFromActive_doesNotCallListener() {
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);

    ResourceWeakReference weakRef = resources.activeEngineResources.get(key);
    resources.deactivate(key);

    enqueueAndWaitForRef(weakRef);

    verify(listener, never()).onResourceReleased(any(Key.class), any(EngineResource.class));
  }

  @Test
  public void queueIdle_withCacheableResourceInActive_callListener() {
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);

    ResourceWeakReference weakRef = resources.activeEngineResources.get(key);
    enqueueAndWaitForRef(weakRef);

    ArgumentCaptor<EngineResource<?>> captor = getEngineResourceCaptor();

    verify(listener).onResourceReleased(eq(key), captor.capture());

    EngineResource<?> released = captor.getValue();
    assertThat(released.getResource()).isEqualTo(resource);
    assertThat(released.isMemoryCacheable()).isTrue();

    released.recycle();
    verify(resource, never()).recycle();
  }

  @Test
  public void queueIdle_withNotCacheableResourceInActive_doesNotCallListener() {
    EngineResource<Object> engineResource = newNonCacheableEngineResource();
    resources.activate(key, engineResource);

    ResourceWeakReference weakRef = resources.activeEngineResources.get(key);
    weakRef.enqueue();
    enqueueAndWaitForRef(weakRef);

    verify(listener, never()).onResourceReleased(any(Key.class), any(EngineResource.class));
  }

  @Test
  public void queueIdle_withCacheableResourceInActive_removesResourceFromActive() {
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);

    ResourceWeakReference weakRef = resources.activeEngineResources.get(key);
    enqueueAndWaitForRef(weakRef);

    assertThat(resources.get(key)).isNull();
  }

  @Test
  public void queueIdle_withNotCacheableResourceInActive_removesResourceFromActive() {
    EngineResource<Object> engineResource = newNonCacheableEngineResource();
    resources.activate(key, engineResource);

    ResourceWeakReference weakRef = resources.activeEngineResources.get(key);
    enqueueAndWaitForRef(weakRef);

    assertThat(resources.get(key)).isNull();
  }

  @Test
  public void queueIdle_withQueuedReferenceRetrievedFromGet_notifiesListener() {
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);

    ResourceWeakReference weakRef = resources.activeEngineResources.get(key);

    resources.get(key);

    enqueueAndWaitForRef(weakRef);

    ArgumentCaptor<EngineResource<?>> captor = getEngineResourceCaptor();
    verify(listener).onResourceReleased(eq(key), captor.capture());
    assertThat(captor.getValue().getResource()).isEqualTo(resource);
  }

  @Test
  public void queueIdle_withQueuedReferenceRetrievedFromGetAndNotCacheable_doesNotNotifyListener() {
    EngineResource<Object> engineResource = newNonCacheableEngineResource();
    resources.activate(key, engineResource);

    ResourceWeakReference weakRef = resources.activeEngineResources.get(key);
    CountDownLatch latch = getLatchForClearedRef();
    weakRef.enqueue();

    resources.get(key);

    waitForLatch(latch);

    verify(listener, never()).onResourceReleased(any(Key.class), any(EngineResource.class));
  }

  @Test
  public void queueIdle_withQueuedReferenceDeactivated_doesNotNotifyListener() {
    final ExecutorService delegate = Executors.newSingleThreadExecutor();
    try {
      final CountDownLatch blockExecutor = new CountDownLatch(1);
      resources =
          new ActiveResources(
              /*isActiveResourceRetentionAllowed=*/ true,
              new Executor() {
                @Override
                public void execute(@NonNull final Runnable command) {
                  delegate.execute(
                      new Runnable() {
                        @Override
                        public void run() {
                          try {
                            blockExecutor.await();
                          } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                          }
                          command.run();
                        }
                      });
                }
              });
      resources.setListener(listener);

      EngineResource<Object> engineResource = newCacheableEngineResource();
      resources.activate(key, engineResource);

      ResourceWeakReference weakRef = resources.activeEngineResources.get(key);
      CountDownLatch latch = getLatchForClearedRef();
      weakRef.enqueue();
      resources.deactivate(key);
      blockExecutor.countDown();

      waitForLatch(latch);

      verify(listener, never()).onResourceReleased(any(Key.class), any(EngineResource.class));
    } finally {
      resources.shutdown();
      com.bumptech.glide.util.Executors.shutdownAndAwaitTermination(delegate);
    }
  }

  @Test
  public void queueIdle_afterReferenceQueuedThenReactivated_doesNotNotifyListener() {
    final ExecutorService delegate = Executors.newSingleThreadExecutor();
    try {
      final CountDownLatch blockExecutor = new CountDownLatch(1);
      resources =
          new ActiveResources(
              /*isActiveResourceRetentionAllowed=*/ true,
              new Executor() {
                @Override
                public void execute(@NonNull final Runnable command) {
                  delegate.execute(
                      new Runnable() {
                        @Override
                        public void run() {
                          try {
                            blockExecutor.await();
                          } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                          }
                          command.run();
                        }
                      });
                }
              });
      resources.setListener(listener);

      EngineResource<Object> first = newCacheableEngineResource();
      resources.activate(key, first);

      ResourceWeakReference weakRef = resources.activeEngineResources.get(key);
      CountDownLatch latch = getLatchForClearedRef();
      weakRef.enqueue();

      EngineResource<Object> second = newCacheableEngineResource();
      resources.activate(key, second);
      blockExecutor.countDown();

      waitForLatch(latch);

      verify(listener, never()).onResourceReleased(any(Key.class), any(EngineResource.class));
    } finally {
      resources.shutdown();
      com.bumptech.glide.util.Executors.shutdownAndAwaitTermination(delegate);
    }
  }

  @Test
  public void activate_withNonCacheableResource_doesNotSaveResource() {
    EngineResource<Object> engineResource = newNonCacheableEngineResource();
    resources.activate(key, engineResource);

    assertThat(resources.activeEngineResources.get(key).resource).isNull();
  }

  @Test
  public void get_withActiveClearedKey_cacheableResource_retentionDisabled_doesNotCallListener() {
    resources = new ActiveResources(/*isActiveResourceRetentionAllowed=*/ false);
    resources.setListener(listener);
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);
    resources.activeEngineResources.get(key).clear();
    resources.get(key);

    verify(listener, never()).onResourceReleased(any(Key.class), any(EngineResource.class));
  }

  @Test
  public void queueIdle_withQueuedReferenceRetrievedFromGet_retentionDisabled_doesNotNotify() {
    resources = new ActiveResources(/*isActiveResourceRetentionAllowed=*/ false);
    resources.setListener(listener);
    EngineResource<Object> engineResource = newCacheableEngineResource();
    resources.activate(key, engineResource);

    ResourceWeakReference weakRef = resources.activeEngineResources.get(key);
    CountDownLatch latch = getLatchForClearedRef();
    weakRef.enqueue();

    resources.get(key);

    waitForLatch(latch);

    verify(listener, never()).onResourceReleased(any(Key.class), any(EngineResource.class));
  }

  private void enqueueAndWaitForRef(ResourceWeakReference ref) {
    CountDownLatch latch = getLatchForClearedRef();
    ref.enqueue();
    waitForLatch(latch);
  }

  private void waitForLatch(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks();
  }

  private CountDownLatch getLatchForClearedRef() {
    final CountDownLatch toWait = new CountDownLatch(1);
    resources.setDequeuedResourceCallback(
        new DequeuedResourceCallback() {
          @Override
          public void onResourceDequeued() {
            toWait.countDown();
          }
        });
    return toWait;
  }

  private EngineResource<Object> newCacheableEngineResource() {
    return new EngineResource<>(
        resource, /*isMemoryCacheable=*/ true, /*isRecyclable=*/ false, key, listener);
  }

  private EngineResource<Object> newNonCacheableEngineResource() {
    return new EngineResource<>(
        resource, /*isMemoryCacheable=*/ false, /*isRecyclable=*/ false, key, listener);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<EngineResource<?>> getEngineResourceCaptor() {
    return (ArgumentCaptor<EngineResource<?>>)
        (ArgumentCaptor<?>) ArgumentCaptor.forClass(EngineResource.class);
  }
}
