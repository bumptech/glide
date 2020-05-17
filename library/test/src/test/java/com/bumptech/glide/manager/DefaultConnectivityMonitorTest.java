package com.bumptech.glide.manager;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class DefaultConnectivityMonitorTest {
  @Mock private ConnectivityStrategy strategy;
  private DefaultConnectivityMonitor monitor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    monitor = new DefaultConnectivityMonitor(strategy);
  }

  @Test
  public void testRegistersOnStart() {
    monitor.onStart();
    verify(strategy).register();
  }

  @Test
  public void testUnregisterOnStop() {
    monitor.onStart();
    monitor.onStop();
    verify(strategy).unregister();
  }
}
