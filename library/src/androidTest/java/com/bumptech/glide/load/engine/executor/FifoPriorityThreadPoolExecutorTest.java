package com.bumptech.glide.load.engine.executor;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.common.testing.EqualsTester;

import com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor.LoadTask;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class FifoPriorityThreadPoolExecutorTest {

  @Test
  public void testLoadsAreExecutedInOrder() throws InterruptedException {
    final List<Integer> resultPriorities = Collections.synchronizedList(new ArrayList<Integer>());
    FifoPriorityThreadPoolExecutor executor = new FifoPriorityThreadPoolExecutor(1);
    for (int i = 5; i > 0; i--) {
      executor.submit(new MockRunnable(i, new MockRunnable.OnRun() {
        @Override
        public void onRun(int priority) {
          resultPriorities.add(priority);
        }
      }));
    }

    executor.shutdown();
    executor.awaitTermination(500, TimeUnit.MILLISECONDS);

    // Since no jobs are queued, the first item added will be run immediately, regardless of
    // priority.
    assertThat(resultPriorities).containsExactly(5, 1, 2, 3, 4);
  }

  @Test
  public void testLoadsWithSamePriorityAreExecutedInSubmitOrder() throws InterruptedException {
    final int numItemsToTest = 10;
    final List<Integer> executionOrder = new ArrayList<>();
    final List<Integer> executedOrder = Collections.synchronizedList(new ArrayList<Integer>());
    FifoPriorityThreadPoolExecutor executor = new FifoPriorityThreadPoolExecutor(1);
    for (int i = 0; i < numItemsToTest; i++) {
      executionOrder.add(i);
    }
    for (int i = 0; i < numItemsToTest; i++) {
      final int finalI = i;
      executor.submit(new MockRunnable(0, new MockRunnable.OnRun() {
        final int position = finalI;

        @Override
        public void onRun(int priority) {
          executedOrder.add(position);
        }
      }));
    }
    executor.awaitTermination(200, TimeUnit.MILLISECONDS);

    assertThat(executedOrder).containsAllIn(executionOrder).inOrder();
  }

  @Test
  public void testLoadTaskEquality() {
    new EqualsTester().addEqualityGroup(new LoadTask<>(new MockRunnable(10), new Object(), 1),
        new LoadTask<>(new MockRunnable(10), new Object(), 1))
        .addEqualityGroup(new LoadTask<>(new MockRunnable(5), new Object(), 1))
        .addEqualityGroup(new LoadTask<>(new MockRunnable(10), new Object(), 3)).testEquals();
  }

  @Test
  public void testLoadTaskCompareToPrefersHigherPriority() {
    LoadTask<Object> first = new LoadTask<>(new MockRunnable(10), new Object(), 10);
    LoadTask<Object> second = new LoadTask<>(new MockRunnable(0), new Object(), 10);

    assertTrue(first.compareTo(second) > 0);
    assertTrue(second.compareTo(first) < 0);
  }

  @Test
  public void testLoadTaskCompareToFallsBackToOrderIfPriorityIsEqual() {
    LoadTask<Object> first = new LoadTask<>(new MockRunnable(0), new Object(), 2);
    LoadTask<Object> second = new LoadTask<>(new MockRunnable(0), new Object(), 1);

    assertTrue(first.compareTo(second) > 0);
    assertTrue(second.compareTo(first) < 0);
  }

  @Test
  public void testLoadTaskCompareToReturnsZeroIfPriorityAndOrderAreEqual() {
    LoadTask<Object> first = new LoadTask<>(new MockRunnable(0), new Object(), 1);
    LoadTask<Object> second = new LoadTask<>(new MockRunnable(0), new Object(), 1);

    assertEquals(0, first.compareTo(second));
    assertEquals(0, second.compareTo(first));
  }

  private static class MockRunnable implements Runnable,
      Prioritized {
    private final int priority;
    private final OnRun onRun;

    public interface OnRun {
      public void onRun(int priority);
    }

    public MockRunnable(int priority) {
      this(priority, mock(OnRun.class));
    }

    public MockRunnable(int priority, OnRun onRun) {
      this.priority = priority;
      this.onRun = onRun;
    }

    @Override
    public int getPriority() {
      return priority;
    }

    @Override
    public void run() {
      onRun.onRun(priority);
    }
  }
}
