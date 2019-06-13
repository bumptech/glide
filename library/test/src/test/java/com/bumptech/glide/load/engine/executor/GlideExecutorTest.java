package com.bumptech.glide.load.engine.executor;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class GlideExecutorTest {

  @Test
  public void testLoadsAreExecutedInOrder() throws InterruptedException {
    final List<Integer> resultPriorities = Collections.synchronizedList(new ArrayList<Integer>());
    GlideExecutor executor = GlideExecutor.newDiskCacheExecutor();
    for (int i = 5; i > 0; i--) {
      executor.execute(
          new MockRunnable(
              i,
              new MockRunnable.OnRun() {
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
    assertThat(resultPriorities).containsExactly(5, 1, 2, 3, 4).inOrder();
  }

  private static final class MockRunnable implements Runnable, Comparable<MockRunnable> {
    private final int priority;
    private final OnRun onRun;

    @Override
    public int compareTo(@NonNull MockRunnable another) {
      return priority - another.priority;
    }

    interface OnRun {
      void onRun(int priority);
    }

    MockRunnable(int priority, OnRun onRun) {
      this.priority = priority;
      this.onRun = onRun;
    }

    @Override
    public void run() {
      onRun.onRun(priority);
    }
  }
}
