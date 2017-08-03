package com.bumptech.glide.tests;

import static org.mockito.Mockito.mock;

import android.os.Looper;
import android.os.MessageQueue;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLooper;

@Implements(Looper.class)
public class GlideShadowLooper extends ShadowLooper {

  @Implementation
  public static MessageQueue myQueue() {
    return mock(MessageQueue.class);
  }
}
