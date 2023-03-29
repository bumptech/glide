package com.bumptech.glide.integration.sqljournaldiskcache;

final class DefaultClock implements Clock {
  @Override
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }
}
