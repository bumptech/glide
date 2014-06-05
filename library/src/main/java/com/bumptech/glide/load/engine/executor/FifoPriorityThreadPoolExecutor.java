package com.bumptech.glide.load.engine.executor;

import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * A FIFO priority {@link ThreadPoolExecutor} that prioritizes submitted {@link Runnable}s by assuming they implement
 * {@link Prioritized}. {@link Prioritized} runnables that return lower values for {@link Prioritized#getPriority()}
 * will be executed before those that return higher values. Priorities only apply when multiple items are queued at the
 * same time. Runnables with the same priority will be executed in FIFO order.
 */
public class FifoPriorityThreadPoolExecutor extends ThreadPoolExecutor {
    AtomicInteger ordering = new AtomicInteger();

    /**
     * Constructor to build a fixed thread pool with the given pool size using {@link DefaultThreadFactory}.
     * @param poolSize The number of threads.
     */
    public FifoPriorityThreadPoolExecutor(int poolSize) {
        this(poolSize, poolSize, 0, TimeUnit.MILLISECONDS, new DefaultThreadFactory());
    }

    public FifoPriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAlive, TimeUnit timeUnit,
            ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAlive, timeUnit, new PriorityBlockingQueue<Runnable>(), threadFactory);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FifoPriorityLoadTask<T>(runnable, value, ordering.getAndIncrement());
    }

    public static class DefaultThreadFactory implements ThreadFactory {
        int threadNum = 0;
        @Override
        public Thread newThread(Runnable runnable) {
              final Thread result = new Thread(runnable, "image-manager-resize-" + threadNum);
                        threadNum++;
                        result.setPriority(THREAD_PRIORITY_BACKGROUND);
                        return result;
        }
    }

    private static class FifoPriorityLoadTask<T> extends FutureTask<T> implements Comparable<FifoPriorityLoadTask> {
        private final int priority;
        private final int order;

        public FifoPriorityLoadTask(Runnable runnable, T result, int order) {
            super(runnable, result);
            if (!(runnable instanceof Prioritized)) {
                throw new IllegalArgumentException("FifoPriorityThreadPoolExecutor must be given Runnables that " +
                        "implement Prioritized");
            }
            priority = ((Prioritized) runnable).getPriority();
            this.order = order;
        }

        @Override
        public int compareTo(FifoPriorityLoadTask loadTask) {
            int result = priority - loadTask.priority;
            if (result == 0 && loadTask != this) {
                result = order - loadTask.order;
            }
            return result;
        }
    }
}
