package com.bumptech.glide.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.RequestManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class RequestManagerFragmentTest {
  private static final String TAG = "tag";
  private SupportRequestManagerHarness harness;

  @Before
  public void setUp() {
    harness = new SupportRequestManagerHarness();
  }

  @Test
  public void testSupportCanSetAndGetRequestManager() {
    RequestManager manager = mock(RequestManager.class);
    harness.setRequestManager(manager);
    assertEquals(manager, harness.getManager());
  }

  @Test
  public void testReturnsLifecycle() {
    assertEquals(harness.getHarnessLifecycle(), harness.getFragmentLifecycle());
  }

  @Test
  public void testDoesNotAddNullRequestManagerToLifecycleWhenSet() {
    harness.setRequestManager(null);
    verify(harness.getHarnessLifecycle(), never()).addListener(any(LifecycleListener.class));
  }

  @Test
  public void testCallsLifecycleStart() {
    harness.getController().start();

    verify(harness.getHarnessLifecycle()).onStart();
  }

  @Test
  public void testCallsRequestManagerStop() {
    harness.getController().start().resume().pause().stop();

    verify(harness.getHarnessLifecycle()).onStop();
  }

  @Test
  public void testCallsRequestManagerDestroy() {
    harness.getController().start().resume().pause().stop().destroy();

    verify(harness.getHarnessLifecycle()).onDestroy();
  }

  @Test
  public void testOnLowMemoryCallOnNullRequestManagerDoesNotCrash() {
    harness.onLowMemory();
  }

  private static class SupportRequestManagerHarness {
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

    public RequestManager getManager() {
      return supportFragment.getRequestManager();
    }

    public void setRequestManager(RequestManager manager) {
      supportFragment.setRequestManager(manager);
    }

    public ActivityFragmentLifecycle getHarnessLifecycle() {
      return lifecycle;
    }

    public ActivityFragmentLifecycle getFragmentLifecycle() {
      return supportFragment.getGlideLifecycle();
    }

    public ActivityController<?> getController() {
      return supportController;
    }

    public void onLowMemory() {
      supportFragment.onLowMemory();
    }
  }
}
