package com.bumptech.glide.integration.sqljournaldiskcache;

import java.time.Duration;

final class TestClock implements Clock {
  private long currentTimeMillis = 0L;

  @Override
  public long currentTimeMillis() {
    return currentTimeMillis;
  }

  void set(long timeMillis) {
    currentTimeMillis = timeMillis;
  }

  void advance(Duration duration) {
    currentTimeMillis += duration.toMillis();
  }
}
