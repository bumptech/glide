package com.bumptech.glide.util;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class FixedPreloadSizeProviderTest {

  // containsExactly doesn't need a return value check.
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void testReturnsGivenSize() {
    int width = 500;
    int height = 1234;
    FixedPreloadSizeProvider<Object> provider = new FixedPreloadSizeProvider<>(width, height);

    int[] size = provider.getPreloadSize(new Object(), 0, 0);

    assertThat(size).asList().containsExactly(width, height);
  }
}
