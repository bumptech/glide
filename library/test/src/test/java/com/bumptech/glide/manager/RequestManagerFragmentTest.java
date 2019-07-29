package com.bumptech.glide.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.RequestManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.exceptions.base.MockitoAssertionError;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class RequestManagerFragmentTest {
  private static final String TAG = "tag";
  private Harness[] harnesses;

  @Before
  public void setUp() {
    harnesses = new Harness[] {new RequestManagerHarness(), new SupportRequestManagerHarness()};
  }

  @Test
  public void testSupportCanSetAndGetRequestManager() {
    runTest(
        new TestCase() {
          @Override
          public void runTest(Harness harness) {
            RequestManager manager = mock(RequestManager.class);
            harness.setRequestManager(manager);
            assertEquals(manager, harness.getManager());
          }
        });
  }

  @Test
  public void testReturnsLifecycle() {
    runTest(
        new TestCase() {
          @Override
          public void runTest(Harness harness) {
            assertEquals(harness.getHarnessLifecycle(), harness.getFragmentLifecycle());
          }
        });
  }

  @Test
  public void testDoesNotAddNullRequestManagerToLifecycleWhenSet() {
    runTest(
        new TestCase() {
          @Override
          public void runTest(Harness harness) {
            harness.setRequestManager(null);
            verify(harness.getHarnessLifecycle(), never())
                .addListener(any(LifecycleListener.class));
          }
        });
  }

  @Test
  public void testCallsLifecycleStart() {
    runTest(
        new TestCase() {
          @Override
          public void runTest(Harness harness) {
            harness.getController().start();

            verify(harness.getHarnessLifecycle()).onStart();
          }
        });
  }

  @Test
  public void testCallsRequestManagerStop() {
    runTest(
        new TestCase() {
          @Override
          public void runTest(Harness harness) {
            harness.getController().start().resume().pause().stop();

            verify(harness.getHarnessLifecycle()).onStop();
          }
        });
  }

  @Test
  public void testCallsRequestManagerDestroy() {
    runTest(
        new TestCase() {
          @Override
          public void runTest(Harness harness) {
            harness.getController().start().resume().pause().stop().destroy();

            verify(harness.getHarnessLifecycle()).onDestroy();
          }
        });
  }

  @Test
  public void testOnLowMemoryCallOnNullRequestManagerDoesNotCrash() {
    runTest(
        new TestCase() {
          @Override
          public void runTest(Harness harness) {
            harness.onLowMemory();
          }
        });
  }

  @Test
  public void testOnTrimMemoryCallOnNullRequestManagerDoesNotCrash() {
    runTest(
        new TestCase() {
          @Override
          public void runTest(Harness harness) {
            harness.onTrimMemory(100 /*level*/);
          }
        });
  }

  private void runTest(TestCase testCase) {
    for (Harness harness : harnesses) {
      try {
        testCase.runTest(harness);
      } catch (MockitoAssertionError e) {
        throw new Error("Failed to get expected call on " + harness, e);
      }
    }
  }

  private interface TestCase {
    void runTest(Harness harness);
  }

  private interface Harness {
    RequestManager getManager();

    void setRequestManager(RequestManager manager);

    ActivityFragmentLifecycle getHarnessLifecycle();

    ActivityFragmentLifecycle getFragmentLifecycle();

    ActivityController<?> getController();

    void onLowMemory();

    void onTrimMemory(@SuppressWarnings("SameParameterValue") int level);
  }

  @SuppressWarnings("deprecation")
  private static class RequestManagerHarness implements Harness {
    private final ActivityController<Activity> controller;
    private final RequestManagerFragment fragment;
    private final ActivityFragmentLifecycle lifecycle = mock(ActivityFragmentLifecycle.class);

    public RequestManagerHarness() {
      fragment = new RequestManagerFragment(lifecycle);
      controller = Robolectric.buildActivity(Activity.class).create();
      controller.get().getFragmentManager().beginTransaction().add(fragment, TAG).commit();
      controller.get().getFragmentManager().executePendingTransactions();
    }

    @Override
    public String toString() {
      return "DefaultHarness";
    }

    @Override
    public RequestManager getManager() {
      return fragment.getRequestManager();
    }

    @Override
    public void setRequestManager(RequestManager requestManager) {
      fragment.setRequestManager(requestManager);
    }

    @Override
    public ActivityFragmentLifecycle getHarnessLifecycle() {
      return lifecycle;
    }

    @Override
    public ActivityFragmentLifecycle getFragmentLifecycle() {
      return fragment.getGlideLifecycle();
    }

    @Override
    public ActivityController<?> getController() {
      return controller;
    }

    @Override
    public void onLowMemory() {
      fragment.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
      fragment.onTrimMemory(level);
    }
  }

  private static class SupportRequestManagerHarness implements Harness {
    private final SupportRequestManagerFragment supportFragment;
    private final ActivityController<FragmentActivity> supportController;
    private final ActivityFragmentLifecycle lifecycle = mock(ActivityFragmentLifecycle.class);

    public SupportRequestManagerHarness() {
      supportFragment = new SupportRequestManagerFragment(lifecycle);
      supportController = Robolectric.buildActivity(FragmentActivity.class).create();

      supportController
          .get()
          .getSupportFragmentManager()
          .beginTransaction()
          .add(supportFragment, TAG)
          .commit();
      supportController.get().getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    public String toString() {
      return "SupportHarness";
    }

    @Override
    public RequestManager getManager() {
      return supportFragment.getRequestManager();
    }

    @Override
    public void setRequestManager(RequestManager manager) {
      supportFragment.setRequestManager(manager);
    }

    @Override
    public ActivityFragmentLifecycle getHarnessLifecycle() {
      return lifecycle;
    }

    @Override
    public ActivityFragmentLifecycle getFragmentLifecycle() {
      return supportFragment.getGlideLifecycle();
    }

    @Override
    public ActivityController<?> getController() {
      return supportController;
    }

    @Override
    public void onLowMemory() {
      supportFragment.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
      // Do nothing.
    }
  }
}
