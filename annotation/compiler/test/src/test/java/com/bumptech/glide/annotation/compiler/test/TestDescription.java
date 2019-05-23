package com.bumptech.glide.annotation.compiler.test;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Exposes the {@link Description} for the current test, similar to {@link
 * org.junit.rules.TestName}.
 */
public final class TestDescription extends TestWatcher {
  private Description description;

  @Override
  protected void starting(Description description) {
    this.description = description;
  }

  public Description getDescription() {
    return description;
  }
}
