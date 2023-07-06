package com.bumptech.glide.manager;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.bumptech.glide.tests.BackgroundUtil.testInBackground;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentController;
import androidx.fragment.app.FragmentHostCallback;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.tests.BackgroundUtil.BackgroundTester;
import com.bumptech.glide.tests.TearDownGlide;
import com.bumptech.glide.tests.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

@LooperMode(LEGACY)
@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class RequestManagerRetrieverTest {
  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();

  private static final String PARENT_TAG = "parent";
  private Context appContext;
  private int initialSdkVersion;
  private RequestManagerRetriever retriever;

  @Before
  public void setUp() {
    appContext = ApplicationProvider.getApplicationContext();

    retriever = new RequestManagerRetriever(/* factory= */ null);

    initialSdkVersion = Build.VERSION.SDK_INT;
    Util.setSdkVersionInt(18);
  }

  @After
  public void tearDown() {
    Util.setSdkVersionInt(initialSdkVersion);

    Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks();
  }

  @Test
  public void testHasValidTag() {
    assertEquals(
        RequestManagerRetriever.class.getPackage().getName(), RequestManagerRetriever.FRAGMENT_TAG);
  }

  @Test
  public void testCanGetRequestManagerFromActivity() {
    Activity activity = Robolectric.buildActivity(Activity.class).create().start().get();
    RequestManager manager = retriever.get(activity);
    assertEquals(manager, retriever.get(activity));
  }

  @Test
  public void testSupportCanGetRequestManagerFromActivity() {
    FragmentActivity fragmentActivity =
        Robolectric.buildActivity(FragmentActivity.class).create().start().get();
    RequestManager manager = retriever.get(fragmentActivity);
    assertEquals(manager, retriever.get(fragmentActivity));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testCanGetRequestManagerFromFragment() {
    Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
    android.app.Fragment fragment = new android.app.Fragment();
    activity.getFragmentManager().beginTransaction().add(fragment, PARENT_TAG).commit();
    activity.getFragmentManager().executePendingTransactions();

    RequestManager manager = retriever.get(fragment);
    assertEquals(manager, retriever.get(fragment));
  }

  @Test
  public void testSupportCanGetRequestManagerFromFragment() {
    FragmentActivity activity =
        Robolectric.buildActivity(FragmentActivity.class).create().start().resume().get();
    Fragment fragment = new Fragment();
    activity.getSupportFragmentManager().beginTransaction().add(fragment, PARENT_TAG).commit();
    activity.getSupportFragmentManager().executePendingTransactions();

    RequestManager manager = retriever.get(fragment);
    assertEquals(manager, retriever.get(fragment));
  }

  @Test
  public void testSupportCanGetRequestManagerFromFragment_nonActivityController() {
    FragmentController controller =
        FragmentController.createController(new NonActivityHostCallback(appContext));
    controller.attachHost(/* fragment= */ null);
    controller.dispatchCreate();
    controller.dispatchStart();
    controller.dispatchResume();

    Fragment fragment = new Fragment();
    controller.getSupportFragmentManager().beginTransaction().add(fragment, PARENT_TAG).commit();
    controller.getSupportFragmentManager().executePendingTransactions();

    RequestManager manager = retriever.get(fragment);
    assertEquals(manager, retriever.get(fragment));
  }

  @Test
  public void testCanGetRequestManagerFromDetachedFragment() {
    helpTestCanGetRequestManagerFromDetachedFragment();
  }

  @Test
  public void testCanGetRequestManagerFromDetachedFragment_PreJellyBeanMr1() {
    Util.setSdkVersionInt(Build.VERSION_CODES.JELLY_BEAN);
    helpTestCanGetRequestManagerFromDetachedFragment();
  }

  @SuppressWarnings("deprecation")
  private void helpTestCanGetRequestManagerFromDetachedFragment() {
    Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
    android.app.Fragment fragment = new android.app.Fragment();
    activity
        .getFragmentManager()
        .beginTransaction()
        .add(fragment, PARENT_TAG)
        .detach(fragment)
        .commit();
    activity.getFragmentManager().executePendingTransactions();

    assertTrue(fragment.isDetached());
    retriever.get(fragment);
  }

  @Test
  public void testSupportCanGetRequestManagerFromDetachedFragment() {
    helpTestSupportCanGetRequestManagerFromDetachedFragment();
  }

  @Test
  public void testSupportCanGetRequestManagerFromDetachedFragment_PreJellyBeanMr1() {
    Util.setSdkVersionInt(Build.VERSION_CODES.JELLY_BEAN);
    helpTestSupportCanGetRequestManagerFromDetachedFragment();
  }

  private void helpTestSupportCanGetRequestManagerFromDetachedFragment() {
    FragmentActivity activity =
        Robolectric.buildActivity(FragmentActivity.class).create().start().resume().get();
    Fragment fragment = new Fragment();
    activity
        .getSupportFragmentManager()
        .beginTransaction()
        .add(fragment, PARENT_TAG)
        .detach(fragment)
        .commit();
    activity.getSupportFragmentManager().executePendingTransactions();

    assertTrue(fragment.isDetached());
    retriever.get(fragment);
  }

  @SuppressWarnings("deprecation")
  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfFragmentNotAttached() {
    android.app.Fragment fragment = new android.app.Fragment();
    retriever.get(fragment);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfSupportFragmentNotAttached() {
    Fragment fragment = new Fragment();
    retriever.get(fragment);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfGivenNullContext() {
    retriever.get((Context) null);
  }

  @Test
  public void testHandlesContextWrappersForApplication() {
    ContextWrapper contextWrapper = new ContextWrapper(appContext);
    RequestManager requestManager = retriever.get(appContext);

    assertEquals(requestManager, retriever.get(contextWrapper));
  }

  @Test
  public void testHandlesContextWrapperWithoutApplication() throws Exception {
    // Create a Context which is not associated with an Application instance.
    Context baseContext =
        appContext.createPackageContext(appContext.getPackageName(), /* flags= */ 0);

    // Sanity-check that Robolectric behaves the same as the framework.
    assertThat(baseContext.getApplicationContext()).isNull();

    // If a wrapper provides a non-null application Context, unwrapping should terminate at this
    // wrapper so that the returned Context has a non-null #getApplicationContext.
    Context contextWithApplicationContext =
        new ContextWrapper(baseContext) {
          @Override
          public Context getApplicationContext() {
            return this;
          }
        };

    Context wrappedContext = new ContextWrapper(contextWithApplicationContext);
    RequestManager requestManager = retriever.get(appContext);

    assertEquals(requestManager, retriever.get(wrappedContext));
  }

  @Test
  public void testReturnsNonNullManagerIfGivenApplicationContext() {
    assertNotNull(retriever.get(appContext));
  }

  @Test
  public void testApplicationRequestManagerIsNotPausedWhenRetrieved() {
    RequestManager manager = retriever.get(appContext);
    assertFalse(manager.isPaused());
  }

  @Test
  public void testApplicationRequestManagerIsNotReResumedAfterFirstRetrieval() {
    RequestManager manager = retriever.get(appContext);
    manager.pauseRequests();
    manager = retriever.get(appContext);
    assertTrue(manager.isPaused());
  }

  @Test
  public void testDoesNotThrowWhenGetWithContextCalledFromBackgroundThread()
      throws InterruptedException {
    testInBackground(
        new BackgroundTester() {
          @Override
          public void runTest() {
            retriever.get(appContext);
          }
        });
  }

  // See Issue #117: https://github.com/bumptech/glide/issues/117.
  @Test
  public void testCanCallGetInOnAttachToWindowInFragmentInViewPager() {
    // Robolectric by default runs messages posted to the main looper synchronously, the
    // framework does not. We post
    // to the main thread here to work around an issue caused by a recursive method call so we
    // need (and reasonably
    // expect) our message to not run immediately
    Shadows.shadowOf(Looper.getMainLooper()).pause();
    Robolectric.buildActivity(Issue117Activity.class).create().start().resume().visible();
  }

  @Test
  @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  public void testDoesNotThrowIfAskedToGetManagerForActivityPreJellYBeanMr1() {
    Util.setSdkVersionInt(Build.VERSION_CODES.JELLY_BEAN);
    Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
    Activity spyActivity = Mockito.spy(activity);
    when(spyActivity.isDestroyed()).thenThrow(new NoSuchMethodError());

    assertNotNull(retriever.get(spyActivity));
  }

  @SuppressWarnings("deprecation")
  @Test
  @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  public void testDoesNotThrowIfAskedToGetManagerForFragmentPreJellyBeanMr1() {
    Util.setSdkVersionInt(Build.VERSION_CODES.JELLY_BEAN);
    Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
    android.app.Fragment fragment = new android.app.Fragment();

    activity.getFragmentManager().beginTransaction().add(fragment, "test").commit();
    android.app.Fragment spyFragment = Mockito.spy(fragment);
    when(spyFragment.getChildFragmentManager()).thenThrow(new NoSuchMethodError());

    assertNotNull(retriever.get(spyFragment));
  }

  @Test
  public void get_beforeActivityIsCreated_returnsSameRequestManagerAsAfterActivityIsCreated() {
    ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
    shadowLooper.pause();
    ActivityController<FragmentActivity> controller =
        Robolectric.buildActivity(FragmentActivity.class);
    RequestManager beforeCreateRequestManager = Glide.with(controller.get());
    // Make sure that the activity makes it one frame without being created.
    controller.create().start();
    // Simulate finishing at least one frame before the next attempt to get a RequestManager
    shadowLooper.runOneTask();

    // Try to get the request manager again. If we've successfully retained the Fragment we wanted
    // to add, then we should get the same instance. If we added a new Fragment instance, the
    // RequestManager won't match and things will be broken.
    RequestManager afterCreateRequestManager = Glide.with(controller.get());
    assertThat(afterCreateRequestManager).isEqualTo(beforeCreateRequestManager);
  }

  @Test
  public void get_onDetachedFragment_returnsSameRequestManagerAsAfterFragmentIsAttached() {
    ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
    shadowLooper.pause();
    ActivityController<FragmentActivity> controller =
        Robolectric.buildActivity(FragmentActivity.class);
    controller.create();

    FragmentActivity fragmentActivity = controller.get();
    Fragment childFragment = new Fragment();
    fragmentActivity
        .getSupportFragmentManager()
        .beginTransaction()
        .add(childFragment, "TEST_TAG")
        .commitNow();
    fragmentActivity
        .getSupportFragmentManager()
        .beginTransaction()
        .detach(childFragment)
        .commitNow();

    RequestManager beforeAttachRequestManager = Glide.with(childFragment);
    shadowLooper.runOneTask();
    fragmentActivity
        .getSupportFragmentManager()
        .beginTransaction()
        .attach(childFragment)
        .commitNow();

    RequestManager afterAttachRequestManager = Glide.with(childFragment);
    assertThat(afterAttachRequestManager).isEqualTo(beforeAttachRequestManager);
  }

  /** Simple callback for creating an Activity-less Fragment host. */
  private final class NonActivityHostCallback
      extends FragmentHostCallback<RequestManagerRetrieverTest> {

    private final Context context;

    NonActivityHostCallback(Context context) {
      super(context, new Handler(Looper.getMainLooper()), /* windowAnimations= */ 0);
      this.context = context;
    }

    @Override
    public LayoutInflater onGetLayoutInflater() {
      return LayoutInflater.from(context).cloneInContext(context);
    }

    @Override
    public RequestManagerRetrieverTest onGetHost() {
      return RequestManagerRetrieverTest.this;
    }
  }
}
