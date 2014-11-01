package com.bumptech.glide.load.engine.executor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class FifoPriorityThreadPoolExecutorTest {

    @Test
    public void testLoadsAreExecutedInOrder() throws InterruptedException {
        final int numPrioritiesToTest = 5;
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

        executor.awaitTermination(200, TimeUnit.MILLISECONDS);

        assertThat(resultPriorities, hasSize(numPrioritiesToTest));

        // Since no jobs are queued, the first item added will be run immediately, regardless of priority.
        assertEquals(numPrioritiesToTest, resultPriorities.get(0).intValue());

        for (int i = 1; i < numPrioritiesToTest; i++) {
            assertEquals(i, resultPriorities.get(i).intValue());
        }
    }

    @Test
    public void testLoadsWithSamePriorityAreExecutedInSubmitOrder() throws InterruptedException {
        final int numItemsToTest = 10;
        final Integer[] executionOrder = new Integer[numItemsToTest];
        final List<Integer> executedOrder = Collections.synchronizedList(new ArrayList<Integer>());
        FifoPriorityThreadPoolExecutor executor = new FifoPriorityThreadPoolExecutor(1);
        for (int i = 0; i < numItemsToTest; i++) {
            executionOrder[i] = i;
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

        assertThat(executedOrder, contains(executionOrder));
    }

    private static class MockRunnable implements Runnable, Prioritized {
        private final int priority;
        private final OnRun onRun;

        public interface OnRun {
            public void onRun(int priority);
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
