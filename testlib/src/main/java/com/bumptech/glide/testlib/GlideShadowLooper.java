package com.bumptech.glide.testlib;

import static org.mockito.Mockito.mock;

import android.os.Looper;
import android.os.MessageQueue;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLooper;

/**
 * A {@link Looper} with a mocked {@link MessageQueue}.
 */
@Implements(Looper.class)
public class GlideShadowLooper extends ShadowLooper {

  @Implementation
  public static MessageQueue myQueue() {
    return mock(MessageQueue.class);
  }
}
