package com.bumptech.glide;

import static com.bumptech.glide.tests.Util.cast;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK)
public class ListPreloaderTest {

  @Mock private RequestBuilder<Object> request;
  @Mock private RequestManager requestManager;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetItemsIsCalledIncreasing() {
    final AtomicBoolean called = new AtomicBoolean(false);
    final AtomicInteger calledCount = new AtomicInteger();

    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            called.set(true);
            final int count = calledCount.getAndIncrement();
            assertEquals(11 + count, position);
            return super.getPreloadItems(position);
          }
        };
    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, 10);
    preloader.onScroll(null, 1, 10, 30);
    assertEquals(10, calledCount.get());
  }

  @Test
  public void testGetItemsIsCalledInOrderIncreasing() {
    final int toPreload = 10;
    final List<Object> objects = new ArrayList<>();
    for (int i = 0; i < toPreload; i++) {
      objects.add(i);
    }

    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          private int expectedPosition;

          @Override
          public int[] getPreloadSize(@NonNull Object item, int adapterPosition, int itemPosition) {
            return new int[] {10, 10};
          }

          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            return objects.subList(position - 11, position + 1 - 11);
          }

          @Nullable
          @Override
          @SuppressWarnings("unchecked")
          public RequestBuilder<Object> getPreloadRequestBuilder(@NonNull Object item) {
            assertEquals(objects.get(expectedPosition), item);
            expectedPosition++;
            return mock(RequestBuilder.class);
          }
        };
    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, toPreload);
    preloader.onScroll(null, 1, 10, 20);
  }

  @Test
  public void testGetItemsIsCalledDecreasing() {
    final AtomicBoolean called = new AtomicBoolean(false);
    final AtomicInteger calledCount = new AtomicInteger();
    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            // Ignore the preload caused from us starting at the end
            if (position >= 40) {
              return Collections.emptyList();
            }
            final int count = calledCount.getAndIncrement();
            called.set(true);
            assertEquals(28 - count, position);
            return super.getPreloadItems(position);
          }
        };
    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, 10);
    preloader.onScroll(null, 30, 10, 40);
    preloader.onScroll(null, 29, 10, 40);
    assertTrue(called.get());
  }

  @Test
  public void testGetItemsIsCalledInOrderDecreasing() {
    final int toPreload = 10;
    final List<Object> objects = new ArrayList<>();
    for (int i = 0; i < toPreload; i++) {
      objects.add(new Object());
    }

    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          private int expectedPosition = toPreload - 1;

          @Override
          public int[] getPreloadSize(@NonNull Object item, int adapterPosition, int itemPosition) {
            return new int[] {10, 10};
          }

          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            if (position == 40) {
              return Collections.emptyList();
            }
            return objects.subList(position, position + 1);
          }

          @Nullable
          @Override
          @SuppressWarnings("unchecked")
          public RequestBuilder<Object> getPreloadRequestBuilder(@NonNull Object item) {
            assertEquals(objects.get(expectedPosition), item);
            expectedPosition--;
            return mock(RequestBuilder.class);
          }
        };
    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, toPreload);
    preloader.onScroll(null, 30, 10, 10);
    preloader.onScroll(null, 29, 10, 10);
  }

  @Test
  public void testGetItemsIsNeverCalledWithEndGreaterThanTotalItems() {
    final AtomicBoolean called = new AtomicBoolean(false);
    final AtomicInteger calledCount = new AtomicInteger();
    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            called.set(true);
            final int count = calledCount.getAndIncrement();
            assertEquals(26 + count, position);
            return super.getPreloadItems(position);
          }
        };
    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, 10);
    preloader.onScroll(null, 16, 10, 30);
    assertTrue(called.get());
  }

  @Test
  public void testGetItemsIsNeverCalledWithStartLessThanZero() {
    final AtomicBoolean called = new AtomicBoolean(false);
    final AtomicInteger calledCount = new AtomicInteger();
    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            if (position >= 17) {
              return Collections.emptyList();
            }
            called.set(true);
            final int count = calledCount.getAndIncrement();
            assertEquals(5 - count, position);
            return super.getPreloadItems(position);
          }
        };

    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, 10);
    preloader.onScroll(null, 7, 10, 30);
    preloader.onScroll(null, 6, 10, 30);
    assertTrue(called.get());
  }

  @Test
  public void testDontPreloadItemsRepeatedlyWhileIncreasing() {
    final AtomicInteger called = new AtomicInteger();
    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            final int current = called.getAndIncrement();
            assertEquals(11 + current, position);
            return super.getPreloadItems(position);
          }
        };

    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, 10);
    preloader.onScroll(null, 1, 10, 30);
    preloader.onScroll(null, 4, 10, 30);

    assertEquals(13, called.get());
  }

  @Test
  public void testDontPreloadItemsRepeatedlyWhileDecreasing() {
    final AtomicInteger called = new AtomicInteger();
    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            if (position >= 20) {
              return Collections.emptyList();
            }
            final int current = called.getAndIncrement();
            assertEquals(19 - current, position);
            return super.getPreloadItems(position);
          }
        };

    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, 10);
    preloader.onScroll(null, 21, 10, 30);
    preloader.onScroll(null, 20, 10, 30);
    preloader.onScroll(null, 17, 10, 30);
    assertEquals(13, called.get());
  }

  @Test
  public void testMultipleItemsForPositionIncreasing() {
    final List<Object> objects = new ArrayList<>();
    objects.add(new Object());
    objects.add(new Object());
    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          private int expectedPosition = (1 + 10) * 2;

          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            return objects;
          }

          @Override
          public int[] getPreloadSize(@NonNull Object item, int adapterPosition, int itemPosition) {
            assertEquals(expectedPosition / 2, adapterPosition);
            assertEquals(expectedPosition % 2, itemPosition);
            expectedPosition++;
            return itemPosition == 0 ? new int[] {10, 11} : new int[] {20, 21};
          }

          @Nullable
          @Override
          public RequestBuilder<Object> getPreloadRequestBuilder(@NonNull Object item) {
            return request;
          }
        };
    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, 10);
    Iterable<Integer> expected = Arrays.asList(10, 11, 20, 21, 10, 11, 20, 21);

    preloader.onScroll(null, 1, 10, 1 + 10 + 2);

    List<Integer> allValues = getTargetsSizes(request, times(4));
    assertEquals(expected, allValues);
  }

  @Test
  public void testMultipleItemsForPositionDecreasing() {
    final List<Object> objects = new ArrayList<>();
    objects.add(new Object());
    objects.add(new Object());
    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          private int expectedPosition = objects.size() * 2 - 1;

          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            return objects;
          }

          @Override
          public int[] getPreloadSize(@NonNull Object item, int adapterPosition, int itemPosition) {
            assertEquals(expectedPosition / 2, adapterPosition);
            assertEquals(expectedPosition % 2, itemPosition);
            expectedPosition--;
            return itemPosition == 0 ? new int[] {10, 11} : new int[] {20, 21};
          }

          @Nullable
          @Override
          public RequestBuilder<Object> getPreloadRequestBuilder(@NonNull Object item) {
            return request;
          }
        };
    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, 10);
    Iterable<Integer> expected = Arrays.asList(20, 21, 10, 11, 20, 21, 10, 11);

    preloader.onScroll(null, 3, 2, 3 + 2);
    preloader.onScroll(null, 2, 2, 3 + 2);

    List<Integer> allValues = getTargetsSizes(request, times(4));
    assertEquals(expected, allValues);
  }

  private <Resource> List<Integer> getTargetsSizes(
      RequestBuilder<Resource> requestBuilder, VerificationMode mode) {
    ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Target<Resource>> targetArgumentCaptor =
        cast(ArgumentCaptor.forClass(Target.class));
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    verify(requestBuilder, mode).into(targetArgumentCaptor.capture());
    for (Target<Resource> target : targetArgumentCaptor.getAllValues()) {
      target.getSize(cb);
    }
    verify(cb, mode).onSizeReady(integerArgumentCaptor.capture(), integerArgumentCaptor.capture());
    return integerArgumentCaptor.getAllValues();
  }

  // It's safe to ignore the return value of containsAllIn.
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void testItemsArePreloadedWithGlide() {
    final List<Object> objects = new ArrayList<>();
    objects.add(new Object());
    objects.add(new Object());
    final HashSet<Object> loadedObjects = new HashSet<>();
    ListPreloaderAdapter preloaderAdapter =
        new ListPreloaderAdapter() {
          @NonNull
          @Override
          public List<Object> getPreloadItems(int position) {
            return objects.subList(position - 11, position - 10);
          }

          @Nullable
          @Override
          public RequestBuilder<Object> getPreloadRequestBuilder(@NonNull Object item) {
            loadedObjects.add(item);
            return super.getPreloadRequestBuilder(item);
          }
        };
    ListPreloader<Object> preloader =
        new ListPreloader<>(requestManager, preloaderAdapter, preloaderAdapter, 10);

    preloader.onScroll(null, 1, 10, 13);
    assertThat(loadedObjects).containsAtLeastElementsIn(objects);
  }

  private static class ListPreloaderAdapter
      implements ListPreloader.PreloadModelProvider<Object>,
          ListPreloader.PreloadSizeProvider<Object> {

    public ListPreloaderAdapter() {}

    @NonNull
    @Override
    public List<Object> getPreloadItems(int position) {
      ArrayList<Object> result = new ArrayList<>(1);
      Collections.fill(result, new Object());
      return result;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public RequestBuilder<Object> getPreloadRequestBuilder(@NonNull Object item) {
      return mock(RequestBuilder.class);
    }

    @Nullable
    @Override
    public int[] getPreloadSize(@NonNull Object item, int adapterPosition, int itemPosition) {
      return new int[] {100, 100};
    }
  }
}
