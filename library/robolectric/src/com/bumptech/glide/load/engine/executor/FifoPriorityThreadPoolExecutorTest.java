package com.bumptech.glide.load.engine.executor;

import com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor;
import com.bumptech.glide.load.engine.executor.Prioritized;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

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

        assertEquals(numPrioritiesToTest, resultPriorities.size());

        // Since no jobs are queued, the first item added will be run immediately, regardless of priority.
        assertEquals(numPrioritiesToTest, resultPriorities.get(0).intValue());

        for (int i = 1; i < numPrioritiesToTest; i++) {
            assertEquals(i, resultPriorities.get(i).intValue());
        }
    }

    @Test
    public void testLoadsWithSamePriorityAreExecutedInSubmitOrder() throws InterruptedException {
        final int numItemsToTest = 10;
        final List<Integer> executionOrder = new ArrayList<Integer>();
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

        assertEquals(numItemsToTest, executedOrder.size());

        for (int i = 0; i < numItemsToTest; i++) {
            assertEquals(executionOrder.get(i), executedOrder.get(i));
        }
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
