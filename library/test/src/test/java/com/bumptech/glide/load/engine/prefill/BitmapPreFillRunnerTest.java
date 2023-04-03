package com.bumptech.glide.load.engine.prefill;

import static com.bumptech.glide.tests.Util.anyResource;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.cache.MemoryCacheAdapter;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.tests.Util.CreateBitmap;
import com.bumptech.glide.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class BitmapPreFillRunnerTest {
  @Mock private BitmapPreFillRunner.Clock clock;
  @Mock private BitmapPool pool;
  @Mock private MemoryCache cache;
  @Mock private Handler mainHandler;
  private final List<Bitmap> addedBitmaps = new ArrayList<>();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    doAnswer(new AddBitmapPoolAnswer(addedBitmaps)).when(pool).put(any(Bitmap.class));
    when(pool.getDirty(anyInt(), anyInt(), any(Bitmap.Config.class)))
        .thenAnswer(new CreateBitmap());
    when(cache.put(any(Key.class), anyResource()))
        .thenAnswer(new AddBitmapCacheAnswer(addedBitmaps));
  }

  private BitmapPreFillRunner getHandler(Map<PreFillType, Integer> allocationOrder) {
    return new BitmapPreFillRunner(
        pool, cache, new PreFillQueue(allocationOrder), clock, mainHandler);
  }

  @Test
  public void testAllocatesABitmapPerSizeInAllocationOrder() {
    PreFillType size = new PreFillType.Builder(100).setConfig(Bitmap.Config.ARGB_8888).build();
    final int toAdd = 3;
    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(size, toAdd);
    BitmapPreFillRunner handler = getHandler(allocationOrder);
    handler.run();

    Bitmap expected = Bitmap.createBitmap(size.getWidth(), size.getHeight(), size.getConfig());
    // TODO(b/20335397): This code was relying on Bitmap equality which Robolectric removed
    // assertThat(addedBitmaps).containsExactly(expected, expected, expected);
  }

  @Test
  public void testAllocatesBitmapsInOrderGivenByAllocationOrder() {
    PreFillType smallWidth =
        new PreFillType.Builder(50, 100).setConfig(Bitmap.Config.ARGB_8888).build();
    PreFillType smallHeight =
        new PreFillType.Builder(100, 50).setConfig(Bitmap.Config.RGB_565).build();

    PreFillType[] expectedOrder =
        new PreFillType[] {
          smallWidth, smallHeight, smallWidth, smallHeight,
        };

    HashMap<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(smallWidth, 2);
    allocationOrder.put(smallHeight, 2);
    BitmapPreFillRunner handler = getHandler(allocationOrder);
    handler.run();

    Bitmap[] expectedBitmaps = new Bitmap[expectedOrder.length];
    for (int i = 0; i < expectedBitmaps.length; i++) {
      PreFillType current = expectedOrder[i];
      expectedBitmaps[i] =
          Bitmap.createBitmap(current.getWidth(), current.getHeight(), current.getConfig());
    }

    Bitmap current = addedBitmaps.get(0);
    for (int i = 1; i < addedBitmaps.size(); i++) {
      assertNotEquals(current, addedBitmaps.get(i));
      current = addedBitmaps.get(i);
    }

    assertThat(addedBitmaps).hasSize(4);
  }

  @Test
  public void testStopsAllocatingBitmapsUntilNextIdleCallIfAllocationsTakeLongerThanLimit() {
    PreFillType size = new PreFillType.Builder(1).setConfig(Bitmap.Config.ARGB_8888).build();
    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(size, 3);
    when(clock.now()).thenReturn(0L).thenReturn(0L).thenReturn(BitmapPreFillRunner.MAX_DURATION_MS);
    BitmapPreFillRunner handler = getHandler(allocationOrder);
    handler.run();

    assertThat(addedBitmaps).hasSize(1);

    handler.run();

    assertThat(addedBitmaps).hasSize(3);
  }

  @Test
  public void testPreFillHandlerDoesNotPostIfHasNoBitmapsToAllocate() {
    BitmapPreFillRunner handler = getHandler(new HashMap<PreFillType, Integer>());
    handler.run();
    verify(mainHandler, never()).postDelayed(any(Runnable.class), anyLong());
  }

  @Test
  public void testPreFillHandlerPostsIfHasBitmapsToAllocateAfterRunning() {
    PreFillType size = new PreFillType.Builder(1).setConfig(Bitmap.Config.ARGB_8888).build();
    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(size, 2);
    BitmapPreFillRunner handler = getHandler(allocationOrder);
    when(clock.now()).thenReturn(0L).thenReturn(0L).thenReturn(BitmapPreFillRunner.MAX_DURATION_MS);

    handler.run();
    verify(mainHandler).postDelayed(eq(handler), anyLong());
  }

  @Test
  public void testPreFillHandlerPostsWithBackoffIfHasBitmapsToAllocateAfterRunning() {
    PreFillType size = new PreFillType.Builder(1).setConfig(Bitmap.Config.ARGB_8888).build();
    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(size, 100);

    BitmapPreFillRunner handler = getHandler(allocationOrder);
    when(clock.now()).thenReturn(0L).thenReturn(0L).thenReturn(BitmapPreFillRunner.MAX_DURATION_MS);

    handler.run();
    verify(mainHandler).postDelayed(eq(handler), eq(BitmapPreFillRunner.INITIAL_BACKOFF_MS));

    when(clock.now())
        .thenReturn(BitmapPreFillRunner.MAX_DURATION_MS)
        .thenReturn(
            BitmapPreFillRunner.MAX_DURATION_MS
                + BitmapPreFillRunner.INITIAL_BACKOFF_MS * BitmapPreFillRunner.BACKOFF_RATIO);

    handler.run();

    verify(mainHandler)
        .postDelayed(
            eq(handler),
            eq(BitmapPreFillRunner.INITIAL_BACKOFF_MS * BitmapPreFillRunner.BACKOFF_RATIO));

    when(clock.now()).thenReturn(0L).thenReturn(BitmapPreFillRunner.MAX_DURATION_MS);
    handler.run();
    when(clock.now()).thenReturn(0L).thenReturn(BitmapPreFillRunner.MAX_DURATION_MS);
    handler.run();
    when(clock.now()).thenReturn(0L).thenReturn(BitmapPreFillRunner.MAX_DURATION_MS);
    handler.run();
    when(clock.now()).thenReturn(0L).thenReturn(BitmapPreFillRunner.MAX_DURATION_MS);
    handler.run();

    verify(mainHandler, atLeastOnce())
        .postDelayed(eq(handler), eq(BitmapPreFillRunner.MAX_BACKOFF_MS));
  }

  @Test
  public void testPreFillHandlerDoesNotPostIfHasBitmapsButIsCancelled() {
    PreFillType size = new PreFillType.Builder(1).setConfig(Bitmap.Config.ARGB_8888).build();
    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(size, 2);

    BitmapPreFillRunner handler = getHandler(allocationOrder);
    when(clock.now()).thenReturn(0L).thenReturn(0L).thenReturn(BitmapPreFillRunner.MAX_DURATION_MS);
    handler.cancel();
    handler.run();

    verify(mainHandler, never()).postDelayed(any(Runnable.class), anyLong());
  }

  @Test
  public void testAddsBitmapsToMemoryCacheIfMemoryCacheHasEnoughSpaceRemaining() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(cache.getMaxSize()).thenReturn(Long.valueOf(Util.getBitmapByteSize(bitmap)));

    PreFillType size =
        new PreFillType.Builder(bitmap.getWidth(), bitmap.getHeight())
            .setConfig(bitmap.getConfig())
            .build();
    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(size, 1);

    getHandler(allocationOrder).run();

    verify(cache).put(any(Key.class), anyResource());
    verify(pool, never()).put(any(Bitmap.class));
    // TODO(b/20335397): This code was relying on Bitmap equality which Robolectric removed
    // assertThat(addedBitmaps).containsExactly(bitmap);
  }

  @Test
  public void testAddsBitmapsToBitmapPoolIfMemoryCacheIsFull() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(cache.getMaxSize()).thenReturn(0L);

    PreFillType size =
        new PreFillType.Builder(bitmap.getWidth(), bitmap.getHeight())
            .setConfig(bitmap.getConfig())
            .build();
    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(size, 1);

    getHandler(allocationOrder).run();

    verify(cache, never()).put(any(Key.class), anyResource());
    // TODO(b/20335397): This code was relying on Bitmap equality which Robolectric removed
    // verify(pool).put(eq(bitmap));
    // assertThat(addedBitmaps).containsExactly(bitmap);
  }

  @Test
  public void testAddsBitmapsToPoolIfMemoryCacheIsNotFullButCannotFitBitmap() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(cache.getMaxSize()).thenReturn((long) Util.getBitmapByteSize(bitmap) / 2);

    PreFillType size =
        new PreFillType.Builder(bitmap.getWidth(), bitmap.getHeight())
            .setConfig(bitmap.getConfig())
            .build();
    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(size, 1);

    getHandler(allocationOrder).run();

    verify(cache, never()).put(any(Key.class), anyResource());
    // TODO(b/20335397): This code was relying on Bitmap equality which Robolectric removed
    // verify(pool).put(eq(bitmap));
    // assertThat(addedBitmaps).containsExactly(bitmap);
  }

  @Test
  public void testDoesAGetFromPoolBeforeAddingForEachSize() {
    Bitmap first = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
    PreFillType firstSize =
        new PreFillType.Builder(first.getWidth(), first.getHeight())
            .setConfig(first.getConfig())
            .build();

    Bitmap second = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565);
    PreFillType secondSize =
        new PreFillType.Builder(second.getWidth(), second.getHeight())
            .setConfig(second.getConfig())
            .build();

    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(firstSize, 1);
    allocationOrder.put(secondSize, 1);

    getHandler(allocationOrder).run();

    InOrder firstOrder = inOrder(pool);
    firstOrder
        .verify(pool)
        .getDirty(eq(first.getWidth()), eq(first.getHeight()), eq(first.getConfig()));
    // TODO(b/20335397): This code was relying on Bitmap equality which Robolectric removed
    // firstOrder.verify(pool).put(eq(first));

    InOrder secondOrder = inOrder(pool);
    secondOrder
        .verify(pool)
        .getDirty(eq(second.getWidth()), eq(second.getHeight()), eq(second.getConfig()));
    // TODO(b/20335397): This code was relying on Bitmap equality which Robolectric removed
    // secondOrder.verify(pool).put(eq(second));
  }

  @Test
  public void testDoesNotGetMoreThanOncePerSize() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
    PreFillType size =
        new PreFillType.Builder(bitmap.getWidth(), bitmap.getHeight())
            .setConfig(bitmap.getConfig())
            .build();

    final int numBitmaps = 5;
    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(size, numBitmaps);

    getHandler(allocationOrder).run();

    InOrder order = inOrder(pool);
    order
        .verify(pool)
        .getDirty(eq(bitmap.getWidth()), eq(bitmap.getHeight()), eq(bitmap.getConfig()));
    // TODO(b/20335397): This code was relying on Bitmap equality which Robolectric removed
    // order.verify(pool, times(numBitmaps)).put(eq(bitmap));
  }

  @Test
  public void allocate_whenBitmapPoolIsAtCapacity_doesNotLogWithRecycledBitmap() {
    ShadowLog.setLoggable(BitmapPreFillRunner.TAG, Log.VERBOSE);

    int dimensions = 10;
    Bitmap.Config config = Bitmap.Config.ARGB_8888;
    int bitmapByteSize = Util.getBitmapByteSize(dimensions, dimensions, config);
    PreFillType preFillType = new PreFillType.Builder(dimensions).setConfig(config).build();
    Map<PreFillType, Integer> allocationOrder = new HashMap<>();
    allocationOrder.put(preFillType, 1);
    PreFillQueue queue = new PreFillQueue(allocationOrder);
    BitmapPreFillRunner runner =
        new BitmapPreFillRunner(
            new LruBitmapPool(bitmapByteSize - 1), new MemoryCacheAdapter(), queue);

    runner.allocate();
  }

  private static final class AddBitmapPoolAnswer implements Answer<Void> {
    private final List<Bitmap> bitmaps;

    AddBitmapPoolAnswer(List<Bitmap> bitmaps) {
      this.bitmaps = bitmaps;
    }

    @Override
    public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
      Bitmap bitmap = (Bitmap) invocationOnMock.getArguments()[0];
      bitmaps.add(bitmap);
      return null;
    }
  }

  private static final class AddBitmapCacheAnswer implements Answer<Resource<?>> {
    private final List<Bitmap> bitmaps;

    AddBitmapCacheAnswer(List<Bitmap> bitmaps) {
      this.bitmaps = bitmaps;
    }

    @Override
    public Resource<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
      BitmapResource resource = (BitmapResource) invocationOnMock.getArguments()[1];
      bitmaps.add(resource.get());
      return null;
    }
  }
}
