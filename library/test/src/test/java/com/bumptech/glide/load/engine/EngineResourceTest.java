package com.bumptech.glide.load.engine;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.bumptech.glide.tests.Util.mockResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.Key;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class EngineResourceTest {
  private EngineResource<Object> engineResource;
  @Mock private EngineResource.ResourceListener listener;
  @Mock private Key cacheKey;
  @Mock private Resource<Object> resource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    engineResource =
        new EngineResource<>(
            resource, /* isMemoryCacheable= */ true, /* isRecyclable= */ true, cacheKey, listener);
  }

  @Test
  public void testCanAcquireAndRelease() {
    engineResource.acquire();
    engineResource.release();

    verify(listener).onResourceReleased(cacheKey, engineResource);
  }

  @Test
  public void testCanAcquireMultipleTimesAndRelease() {
    engineResource.acquire();
    engineResource.acquire();
    engineResource.release();
    engineResource.release();

    verify(listener).onResourceReleased(eq(cacheKey), eq(engineResource));
  }

  @Test
  public void testDelegatesGetToWrappedResource() {
    Object expected = new Object();
    when(resource.get()).thenReturn(expected);
    assertEquals(expected, engineResource.get());
  }

  @Test
  public void testDelegatesGetSizeToWrappedResource() {
    int expectedSize = 1234;
    when(resource.getSize()).thenReturn(expectedSize);
    assertEquals(expectedSize, engineResource.getSize());
  }

  @Test
  public void testRecyclesWrappedResourceWhenRecycled() {
    engineResource.acquire();
    engineResource.release();
    engineResource.recycle();
    verify(resource).recycle();
  }

  @Test(expected = IllegalStateException.class)
  public void testThrowsIfRecycledTwice() {
    engineResource.recycle();
    engineResource.recycle();
  }

  @Test(expected = IllegalStateException.class)
  public void testThrowsIfReleasedBeforeAcquired() {
    engineResource.release();
  }

  @Test(expected = IllegalStateException.class)
  public void testThrowsIfRecycledWhileAcquired() {
    engineResource.acquire();
    engineResource.recycle();
  }

  @Test(expected = IllegalStateException.class)
  public void testThrowsIfAcquiredAfterRecycled() {
    engineResource.recycle();
    engineResource.acquire();
  }

  @Test
  public void testThrowsIfAcquiredOnBackgroundThread() throws InterruptedException {
    Thread otherThread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  engineResource.acquire();
                } catch (IllegalThreadStateException e) {
                  return;
                }
                fail("Failed to receive expected IllegalThreadStateException");
              }
            });
    otherThread.start();
    otherThread.join();
  }

  @Test
  public void testThrowsIfReleasedOnBackgroundThread() throws InterruptedException {
    engineResource.acquire();
    Thread otherThread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  engineResource.release();
                } catch (IllegalThreadStateException e) {
                  return;
                }
                fail("Failed to receive expected IllegalThreadStateException");
              }
            });
    otherThread.start();
    otherThread.join();
  }

  @Test(expected = IllegalStateException.class)
  public void testThrowsIfReleasedMoreThanAcquired() {
    engineResource.acquire();
    engineResource.release();
    engineResource.release();
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfWrappedResourceIsNull() {
    new EngineResource<>(
        /* toWrap= */ null,
        /* isMemoryCacheable= */ false,
        /* isRecyclable= */ true,
        cacheKey,
        listener);
  }

  @Test
  public void testCanSetAndGetIsCacheable() {
    engineResource =
        new EngineResource<>(
            mockResource(),
            /* isMemoryCacheable= */ true,
            /* isRecyclable= */ true,
            cacheKey,
            listener);
    assertTrue(engineResource.isMemoryCacheable());
    engineResource =
        new EngineResource<>(
            mockResource(),
            /* isMemoryCacheable= */ false,
            /* isRecyclable= */ true,
            cacheKey,
            listener);
    assertFalse(engineResource.isMemoryCacheable());
  }

  @Test
  public void release_whenNotRecycleable_doesNotRecycleResource() {
    resource = mockResource();
    engineResource =
        new EngineResource<>(
            resource, /* isMemoryCacheable= */ true, /* isRecyclable= */ false, cacheKey, listener);
    engineResource.recycle();

    verify(listener, never()).onResourceReleased(any(Key.class), any(EngineResource.class));
    verify(resource, never()).recycle();
  }
}
