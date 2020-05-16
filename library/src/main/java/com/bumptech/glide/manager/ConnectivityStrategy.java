package com.bumptech.glide.manager;

/** A strategy that to perceive connectivity changes. */
public interface ConnectivityStrategy {
  /** perceive connectivity changes from Framework. */
  void register();

  /** stop perceive connectivity changes from Framework. */
  void unregister();
}
