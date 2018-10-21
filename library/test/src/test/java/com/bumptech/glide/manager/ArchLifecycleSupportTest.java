package com.bumptech.glide.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.tests.GlideShadowLooper;
import com.bumptech.glide.tests.TearDownGlide;
import com.bumptech.glide.tests.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18, shadows = GlideShadowLooper.class)
public class ArchLifecycleSupportTest {

  @Rule
  public TearDownGlide tearDownGlide = new TearDownGlide();

  private RequestManagerRetriever retriever;

  private LifecycleRegistry lifecycle;

  private ArchLifecycleOwner lifecycleOwner;

  private int initialSdkVersion;

  @Before
  public void setUp() {
    retriever = new RequestManagerRetriever(new RequestManagerRetriever.RequestManagerFactory() {
      @NonNull
      @Override
      public RequestManager build(@NonNull Glide glide, @NonNull Lifecycle lifecycle,
                                  @NonNull RequestManagerTreeNode requestManagerTreeNode,
                                  @NonNull Context context) {
        RequestManager requestManager = mock(RequestManager.class);
        lifecycle.addListener(requestManager);
        return requestManager;
      }
    });
    lifecycle = new TestLifecycleOwner().registry;
    lifecycleOwner = new ArchLifecycleOwner(RuntimeEnvironment.application, lifecycle);
    initialSdkVersion = Build.VERSION.SDK_INT;
    lifecycle.markState(android.arch.lifecycle.Lifecycle.State.CREATED);
    Util.setSdkVersionInt(18);
  }

  @Test
  public void testReturnsNewManagerIfNoneExists() {
    assertNotNull(retriever.get(lifecycleOwner));
  }

  @Test
  public void testReturnsExistingRequestManagerIfExists() {
    RequestManager requestManager = retriever.get(lifecycleOwner);
    assertEquals(requestManager, retriever.get(lifecycleOwner));
  }

  @Test
  public void testNotifyRequestManagerLifecycle() {
    RequestManager requestManager = retriever.get(lifecycleOwner);
    lifecycle.handleLifecycleEvent(Event.ON_START);
    verify(requestManager).onStart();

    lifecycle.handleLifecycleEvent(Event.ON_STOP);
    verify(requestManager).onStop();

    lifecycle.handleLifecycleEvent(Event.ON_DESTROY);
    verify(requestManager).onDestroy();
  }

  @Test
  public void testNotifyLifecycleEventsAddedLater() {
    ArchLifecycle archLifecycle = new ArchLifecycle(lifecycle);
    lifecycle.handleLifecycleEvent(Event.ON_START);
    LifecycleListener listener = mock(LifecycleListener.class);
    archLifecycle.addListener(listener);
    verify(listener).onStart();
  }

  @After
  public void tearDown() {
    Util.setSdkVersionInt(initialSdkVersion);
  }

  private static class TestLifecycleOwner implements LifecycleOwner {

    final LifecycleRegistry registry = new LifecycleRegistry(this) {
      @Override
      public void addObserver(@NonNull LifecycleObserver observer) {
        super.addObserver(observer);
      }
    };

    @NonNull
    @Override
    public android.arch.lifecycle.Lifecycle getLifecycle() {
      return registry;
    }

  }

}
