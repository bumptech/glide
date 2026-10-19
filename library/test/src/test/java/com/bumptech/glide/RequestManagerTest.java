package com.bumptech.glide;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.bumptech.glide.tests.BackgroundUtil.testInBackground;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.manager.ConnectivityMonitor;
import com.bumptech.glide.manager.ConnectivityMonitor.ConnectivityListener;
import com.bumptech.glide.manager.ConnectivityMonitorFactory;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestManagerTreeNode;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.tests.BackgroundUtil;
import com.bumptech.glide.tests.TearDownGlide;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class RequestManagerTest {
  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();

  @Mock private Lifecycle lifecycle = mock(Lifecycle.class);
  @Mock private RequestManagerTreeNode treeNode = mock(RequestManagerTreeNode.class);

  private RequestManager manager;
  private ConnectivityMonitor connectivityMonitor;
  private RequestTracker requestTracker;
  private ConnectivityListener connectivityListener;
  private Application context;
  private CustomTarget<Drawable> target;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();
    connectivityMonitor = mock(ConnectivityMonitor.class);
    ConnectivityMonitorFactory factory = mock(ConnectivityMonitorFactory.class);
    when(factory.build(isA(Context.class), isA(ConnectivityMonitor.ConnectivityListener.class)))
        .thenAnswer(
            new Answer<ConnectivityMonitor>() {
              @Override
              public ConnectivityMonitor answer(InvocationOnMock invocation) {
                connectivityListener = (ConnectivityListener) invocation.getArguments()[1];
                return connectivityMonitor;
              }
            });

    target =
        new CustomTarget<Drawable>() {
          @Override
          public void onResourceReady(
              @NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
            // Empty.
          }

          @Override
          public void onLoadCleared(@Nullable Drawable placeholder) {}
        };

    requestTracker = mock(RequestTracker.class);
    manager =
        new RequestManager(
            Glide.get(ApplicationProvider.getApplicationContext()),
            lifecycle,
            treeNode,
            requestTracker,
            factory,
            context);
  }

  @Test
  public void testPauseRequestsPausesRequests() {
    manager.pauseRequests();

    verify(requestTracker).pauseRequests();
  }

  @Test
  public void testResumeRequestsResumesRequests() {
    manager.resumeRequests();

    verify(requestTracker).resumeRequests();
  }

  @Test
  public void testPausesRequestsOnStop() {
    manager.onStart();
    manager.onStop();

    verify(requestTracker).pauseRequests();
  }

  @Test
  public void testResumesRequestsOnStart() {
    manager.onStart();

    verify(requestTracker).resumeRequests();
  }

  @Test
  public void testClearsRequestsOnDestroy() {
    manager.onDestroy();

    verify(requestTracker).clearRequests(true);
  }

  @Test
  public void testAddsConnectivityMonitorToLifecycleWhenConstructed() {
    verify(lifecycle).addListener(eq(connectivityMonitor));
  }

  @Test
  public void testAddsSelfToLifecycleWhenConstructed() {
    verify(lifecycle).addListener(eq(manager));
  }

  @Test
  public void testRestartsRequestOnConnected() {
    connectivityListener.onConnectivityChanged(true);

    verify(requestTracker).restartRequests();
  }

  @Test
  public void testDoesNotRestartRequestsOnDisconnected() {
    connectivityListener.onConnectivityChanged(false);

    verify(requestTracker, never()).restartRequests();
  }

  @Test
  public void resumeRequests_whenCalledOnBackgroundThread_doesNotThrow()
      throws InterruptedException {
    testInBackground(
        new BackgroundUtil.BackgroundTester() {
          @Override
          public void runTest() {
            manager.resumeRequests();
          }
        });
  }

  @Test
  public void pauseRequests_whenCalledOnBackgroundThread_doesNotThrow()
      throws InterruptedException {
    testInBackground(
        new BackgroundUtil.BackgroundTester() {
          @Override
          public void runTest() {
            manager.pauseRequests();
          }
        });
  }

  @Test
  public void testDelegatesIsPausedToRequestTracker() {
    when(requestTracker.isPaused()).thenReturn(true);
    assertTrue(manager.isPaused());
    when(requestTracker.isPaused()).thenReturn(false);
    assertFalse(manager.isPaused());
  }

  @Test
  public void clear_withRequestStartedInSiblingManager_doesNotThrow() {
    final RequestManager child1 =
        new RequestManager(
            Glide.get(context),
            lifecycle,
            new RequestManagerTreeNode() {
              @NonNull
              @Override
              public Set<RequestManager> getDescendants() {
                return Collections.emptySet();
              }
            },
            context);
    final RequestManager child2 =
        new RequestManager(
            Glide.get(context),
            lifecycle,
            new RequestManagerTreeNode() {
              @NonNull
              @Override
              public Set<RequestManager> getDescendants() {
                return Collections.emptySet();
              }
            },
            context);
    new RequestManager(
        Glide.get(context),
        lifecycle,
        new RequestManagerTreeNode() {
          @NonNull
          @Override
          public Set<RequestManager> getDescendants() {
            return new HashSet<>(java.util.Arrays.asList(child1, child2));
          }
        },
        context);

    File file = new File("fake");
    child1.load(file).into(target);
    child2.clear(target);
  }

  @Test
  public void clear_withRequestStartedInChildManager_doesNotThrow() {
    final RequestManager child =
        new RequestManager(
            Glide.get(context),
            lifecycle,
            new RequestManagerTreeNode() {
              @NonNull
              @Override
              public Set<RequestManager> getDescendants() {
                return Collections.emptySet();
              }
            },
            context);
    RequestManager parent =
        new RequestManager(
            Glide.get(context),
            lifecycle,
            new RequestManagerTreeNode() {
              @NonNull
              @Override
              public Set<RequestManager> getDescendants() {
                return Collections.singleton(child);
              }
            },
            context);

    File file = new File("fake");
    child.load(file).into(target);
    parent.clear(target);
  }

  /**
   * Reproduces the ANR reported in https://github.com/bumptech/glide/issues/5398.
   *
   * <p>When RequestManager.onDestroy() is called, it iterates all tracked targets and calls
   * clear() on each. For requests configured with a placeholder resource ID (not a pre-loaded
   * Drawable), SingleRequest.clear() triggers synchronous drawable resource decoding via
   * getPlaceholderDrawable() -> loadDrawable() -> DrawableDecoderCompat.getDrawable(). On a real
   * device, this involves native image decoding (ImageDecoder.nDecodeBitmap) on the main thread.
   * With multiple requests, the cumulative cost causes ANRs.
   */
  @Test
  public void onDestroy_withPlaceholderResourceId_loadsDrawableSynchronouslyDuringClear() {
    final Drawable[] clearedPlaceholder = {null};
    CustomTarget<Drawable> targetWithPlaceholderId =
        new CustomTarget<>() {
          @Override
          public void onResourceReady(
              @NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {}

          @Override
          public void onLoadCleared(@Nullable Drawable placeholder) {
            clearedPlaceholder[0] = placeholder;
          }
        };

    RequestManager managerWithRealTracker =
        new RequestManager(
            Glide.get(context),
            lifecycle,
            Collections::emptySet,
            context);

    // Load with a placeholder RESOURCE ID (not a Drawable object).
    // This is the key condition: the placeholder drawable is lazily loaded during clear().
    managerWithRealTracker
        .load(new File("fake"))
        .placeholder(android.R.drawable.ic_delete)
        .into(targetWithPlaceholderId);

    // Simulate Activity destruction. This calls clearRequests() which calls clear() on each
    // tracked target, triggering synchronous drawable loading via DrawableDecoderCompat.
    managerWithRealTracker.onDestroy();

    // During onDestroy(), there is no need to load a placeholder drawable since the Activity is
    // being destroyed. Loading it synchronously causes ANRs (see #5398). The fix should skip
    // placeholder loading during destruction, so onLoadCleared() should receive null.
    assertThat(clearedPlaceholder[0]).isNull();
  }

  /**
   * Simulates the ANR multiplication effect: RequestManager.onDestroy() with N requests that each
   * have a placeholder resource ID results in N synchronous drawable decode operations on the main
   * thread.
   */
  @Test
  public void onDestroy_multipleRequestsWithPlaceholderResourceId_allLoadDrawablesSynchronously() {
    int numRequests = 10;
    final Drawable[][] clearedPlaceholders = new Drawable[numRequests][1];

    RequestManager managerWithRealTracker =
        new RequestManager(
            Glide.get(context),
            lifecycle,
            Collections::emptySet,
            context);

    for (int i = 0; i < numRequests; i++) {
      final int index = i;
      CustomTarget<Drawable> t =
          new CustomTarget<>() {
            @Override
            public void onResourceReady(
                @NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {}

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
              clearedPlaceholders[index][0] = placeholder;
            }
          };

      managerWithRealTracker
          .load(new File("fake"))
          .placeholder(android.R.drawable.ic_delete)
          .into(t);
    }

    // onDestroy iterates all N targets, calling clear() on each.
    // Each clear() synchronously loads the placeholder drawable.
    managerWithRealTracker.onDestroy();

    // During onDestroy(), placeholder drawables should NOT be loaded (causes ANRs, see #5398).
    // The fix should skip placeholder loading during destruction.
    for (int i = 0; i < numRequests; i++) {
      assertThat(clearedPlaceholders[i][0]).isNull();
    }
  }

  @Test
  public void clear_withRequestStartedInParentManager_doesNotThrow() {
    final RequestManager child =
        new RequestManager(
            Glide.get(context),
            lifecycle,
            new RequestManagerTreeNode() {
              @NonNull
              @Override
              public Set<RequestManager> getDescendants() {
                return Collections.emptySet();
              }
            },
            context);
    RequestManager parent =
        new RequestManager(
            Glide.get(context),
            lifecycle,
            new RequestManagerTreeNode() {
              @NonNull
              @Override
              public Set<RequestManager> getDescendants() {
                return Collections.singleton(child);
              }
            },
            context);

    File file = new File("fake");

    parent.load(file).into(target);
    child.clear(target);
  }
}
