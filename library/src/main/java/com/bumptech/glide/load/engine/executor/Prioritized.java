package com.bumptech.glide.load.engine.executor;

/**
 * A simple interface for exposing the priority of a task. Lower integer values are treated as having higher priority
 * with 0 being the highest priority possible.
 */
public interface Prioritized {
    /**
     * Returns the priority of this task.
     */
    int getPriority();
}
