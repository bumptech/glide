package com.bumptech.glide;

/**
 * Priorities for completing loads. If more than one load is queued at a time, the load with the
 * higher priority will be started first. Priorities are considered best effort, there are no
 * guarantees about the order in which loads will start or finish.
 */
public enum Priority {
  IMMEDIATE,
  HIGH,
  NORMAL,
  LOW,
}
