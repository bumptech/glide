package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ActivityScenario.ActivityAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.instrumentation.R;
import com.bumptech.glide.test.DefaultFragmentActivity;
import com.bumptech.glide.testutil.TearDownGlide;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// This test avoids using FragmentScenario because it doesn't seem to let us to get into the common
// created but not yet started state, only either before onCreateView or after onResume.
@RunWith(AndroidJUnit4.class)
public class RequestManagerLifecycleTest {
  private static final String FRAGMENT_TAG = "fragment";
  private static final String FRAGMENT_SIBLING_TAG = "fragment_sibling";
  private static final String CHILD_FRAGMENT_TAG = "child";
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();

  @Rule
  public final ActivityScenarioRule<DefaultFragmentActivity> scenarioRule =
      new ActivityScenarioRule<>(DefaultFragmentActivity.class);

  private ActivityScenario<DefaultFragmentActivity> scenario;

  @Before
  public void setUp() {
    scenario = scenarioRule.getScenario();
  }

  @Test
  public void get_twice_withSameActivity_returnsSameRequestManager() {
    scenario.moveToState(State.CREATED);
    scenario.onActivity(
        activity -> assertThat(Glide.with(activity)).isEqualTo(Glide.with(activity)));
  }

  @Test
  public void get_withActivityBeforeCreate_startsRequestManager() {
    scenario.moveToState(State.CREATED);
    scenario.onActivity(activity -> assertThat(Glide.with(activity).isPaused()).isFalse());
  }

  // See b/262668610
  @SuppressWarnings("OnLifecycleEvent")
  @Test
  public void get_withActivityOnDestroy_QPlus_doesNotCrash() {
    // Activity#isDestroyed's behavior seems to have changed in Q. On Q+, isDestroyed returns false
    // during onDestroy, so we have to handle that case explicitly.
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
    scenario.moveToState(State.CREATED);

    class GetOnDestroy implements LifecycleObserver {
      private final FragmentActivity activity;

      GetOnDestroy(FragmentActivity activity) {
        this.activity = activity;
      }

      @OnLifecycleEvent(Event.ON_DESTROY)
      public void onDestroy(@NonNull LifecycleOwner owner) {
        Glide.with(activity);
      }
    }
    scenario.onActivity(
        activity -> activity.getLifecycle().addObserver(new GetOnDestroy(activity)));
    scenario.moveToState(State.DESTROYED);
  }

  @SuppressWarnings("OnLifecycleEvent")
  @Test
  public void get_withActivityOnDestroy_afterJellyBeanAndbeforeQ_doesNotCrash() {
    // Activity#isDestroyed's behavior seems to have changed in Q. On <Q, isDestroyed returns true
    // during onDestroy, triggering an assertion in Glide. < Jelly bean, isDestroyed is not
    // available as a method.
    assumeTrue(
        Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN
            && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q);
    AtomicReference<Exception> thrownException = new AtomicReference<>();
    scenario.moveToState(State.CREATED);

    class GetOnDestroy implements LifecycleObserver {
      private final FragmentActivity activity;

      GetOnDestroy(FragmentActivity activity) {
        this.activity = activity;
      }

      @OnLifecycleEvent(Event.ON_DESTROY)
      public void onDestroy(@NonNull LifecycleOwner owner) {
        try {
          Glide.with(activity);
          fail("Failed to throw expected exception");
        } catch (Exception e) {
          thrownException.set(e);
        }
      }
    }
    scenario.onActivity(
        activity -> activity.getLifecycle().addObserver(new GetOnDestroy(activity)));
    scenario.moveToState(State.DESTROYED);

    assertThat(thrownException.get())
        .hasMessageThat()
        .contains("You cannot start a load for a destroyed activity");
  }

  @Test
  public void get_withFragment_beforeFragmentIsAdded_throws() {
    Fragment fragment = new Fragment();
    assertThrows(NullPointerException.class, () -> Glide.with(fragment));
  }

  @Test
  public void get_withFragment_whenFragmentIsAddedAndVisible_beforeStart_startsRequestManager() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment fragment = getFragment(activity);

          assertThat(fragment.isVisible()).isTrue();
          assertThat(Glide.with(fragment).isPaused()).isFalse();
        });
  }

  @Test
  public void requestManager_afterFragmentIsStopped_isPaused() {
    // Avoid using FragmentScenario because it doesn't seem to let us to get into the common created
    // but not yet started state, only either before onCreateView or after onResume.
    final Fragment fragment = new EmptyContainerFragment();
    scenario.moveToState(State.RESUMED);
    scenario.onActivity(
        activity -> {
          activity
              .getSupportFragmentManager()
              .beginTransaction()
              .add(R.id.container, fragment)
              .commitNowAllowingStateLoss();
          // If we call with() for the first time after the fragment is paused but while it's still
          // visible, then we'll default the request manager to started. So we call with() once here
          // to make sure the request manager is created before the stop event below.
          Glide.with(fragment);
        });

    scenario.moveToState(State.CREATED);
    scenario.onActivity(
        activity -> {
          assertThat(fragment.isVisible()).isTrue();
          assertThat(Glide.with(fragment).isPaused()).isTrue();
        });
  }

  @Test
  public void get_twice_withSameFragment_returnsSameRequestManager() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment fragment = getFragment(activity);
          assertThat(Glide.with(fragment)).isEqualTo(Glide.with(fragment));
        });
  }

  @Test
  public void pauseRequestsRecursive_onActivity_pausesFragment() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment fragment = getFragment(activity);

          Glide.with(activity).pauseAllRequestsRecursive();
          assertThat(Glide.with(fragment).isPaused()).isTrue();
        });
  }

  @Test
  public void resumeRequestsRecursive_onActivity_resumesFragment() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment fragment = getFragment(activity);

          Glide.with(activity).pauseAllRequestsRecursive();
          Glide.with(activity).resumeRequestsRecursive();

          assertThat(Glide.with(fragment).isPaused()).isFalse();
        });
  }

  @Test
  public void pauseRequestsRecursive_onActivity_pausesChildOfChildFragment() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment childFragment = getChildFragment(activity);

          Glide.with(activity).pauseAllRequestsRecursive();

          assertThat(Glide.with(childFragment).isPaused()).isTrue();
        });
  }

  @Test
  public void resumeRequestsRecursive_onActivity_resumesChildOfChildFragment() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment childFragment = getChildFragment(activity);

          Glide.with(activity).pauseAllRequestsRecursive();
          Glide.with(activity).resumeRequestsRecursive();

          assertThat(Glide.with(childFragment).isPaused()).isFalse();
        });
  }

  @Test
  public void pauseRequestsRecursive_onChildFragmentOfActivity_doesNotPauseActivity() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment fragment = getFragment(activity);

          Glide.with(fragment).pauseAllRequestsRecursive();

          assertThat(Glide.with(fragment).isPaused()).isTrue();
          assertThat(Glide.with(activity).isPaused()).isFalse();
        });
  }

  @Test
  public void pauseRequestsRecursive_onChildFragmentOfActivity_pausesChildOfChildFragment() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment parentFragment = getFragment(activity);
          Fragment childFragment = getChildFragment(activity);

          Glide.with(parentFragment).pauseAllRequestsRecursive();

          assertThat(Glide.with(childFragment).isPaused()).isTrue();
        });
  }

  @Test
  public void resumeRequestsRecursive_onChildFragmentOfActivity_resumesChildOfChildFragment() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment parentFragment = getFragment(activity);
          Fragment childFragment = getChildFragment(activity);

          Glide.with(parentFragment).pauseAllRequestsRecursive();
          Glide.with(parentFragment).resumeRequestsRecursive();

          assertThat(Glide.with(childFragment).isPaused()).isFalse();
        });
  }

  @Test
  public void pauseRequests_onActivity_pausesRequestManager() {
    scenario.moveToState(State.RESUMED);
    scenario.onActivity(
        activity -> {
          Glide.with(activity).pauseAllRequests();
          assertThat(Glide.with(activity).isPaused()).isTrue();
        });
  }

  @Test
  public void resumeRequests_onActivity_pausesRequestManager() {
    scenario.moveToState(State.RESUMED);
    scenario.onActivity(
        activity -> {
          Glide.with(activity).pauseAllRequests();
          Glide.with(activity).resumeRequests();
          assertThat(Glide.with(activity).isPaused()).isFalse();
        });
  }

  @Test
  public void pauseRequests_onActivity_doesNotPauseChildren() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment fragment = getFragment(activity);
          initRequestManagers(activity, fragment);

          Glide.with(activity).pauseAllRequests();
          assertThat(Glide.with(fragment).isPaused()).isFalse();
        });
  }

  @Test
  public void resumeRequests_onActivity_doesNotResumeChildren() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment fragment = getFragment(activity);
          initRequestManagers(activity, fragment);

          Glide.with(activity).pauseAllRequests();
          Glide.with(fragment).pauseAllRequests();
          Glide.with(activity).resumeRequests();

          assertThat(Glide.with(fragment).isPaused()).isTrue();
        });
  }

  @Test
  public void pauseRequests_onFragment_pausesRequestManager() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment fragment = getFragment(activity);
          Glide.with(fragment).pauseAllRequests();
          assertThat(Glide.with(fragment).isPaused()).isTrue();
        });
  }

  @Test
  public void resumeRequests_onFragment_resumesRequestManager() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment fragment = getFragment(activity);
          Glide.with(fragment).pauseAllRequests();
          Glide.with(fragment).resumeRequests();
          assertThat(Glide.with(fragment).isPaused()).isFalse();
        });
  }

  @Test
  public void pauseRequests_onChildFragment_doesNotPauseParentFragment() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Glide.with(getChildFragment(activity)).pauseAllRequests();

          assertThat(Glide.with(getFragment(activity)).isPaused()).isFalse();
        });
  }

  @Test
  public void resumeRequests_onChildFragment_doesNotResumeParentFragment() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment parentFragment = getFragment(activity);
          Fragment childFragment = getChildFragment(activity);
          Glide.with(childFragment).pauseAllRequests();
          Glide.with(parentFragment).pauseAllRequests();
          Glide.with(childFragment).resumeRequests();

          assertThat(Glide.with(parentFragment).isPaused()).isTrue();
        });
  }

  @Test
  public void pauseRequests_onChildFragment_pausesChildFragment() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment childFragment = getChildFragment(activity);
          Glide.with(childFragment).pauseAllRequests();

          assertThat(Glide.with(childFragment).isPaused()).isTrue();
        });
  }

  @Test
  public void resumeRequests_onChildFragment_resumesChildFragment() {
    withActivityFragmentAndChildFragment(
        activity -> {
          Fragment childFragment = getChildFragment(activity);
          Glide.with(childFragment).pauseAllRequests();
          Glide.with(childFragment).resumeRequests();

          assertThat(Glide.with(childFragment).isPaused()).isFalse();
        });
  }

  @Test
  public void pauseRequestsRecursive_onActivity_withTwoSiblingFragments_pausesBothSiblings() {
    withActivityAndTwoFragmentSiblings(
        activity -> {
          Fragment fragment = getFragment(activity);
          Fragment sibling = getSiblingFragment(activity);

          Glide.with(activity).pauseAllRequestsRecursive();

          assertThat(Glide.with(fragment).isPaused()).isTrue();
          assertThat(Glide.with(sibling).isPaused()).isTrue();
        });
  }

  @Test
  public void resumeRequestsRecursive_onActivity_withTwoSiblingFragments_resumesBothSiblings() {
    withActivityAndTwoFragmentSiblings(
        activity -> {
          Fragment fragment = getFragment(activity);
          Fragment sibling = getSiblingFragment(activity);

          Glide.with(activity).pauseAllRequestsRecursive();
          Glide.with(activity).resumeRequestsRecursive();

          assertThat(Glide.with(fragment).isPaused()).isFalse();
          assertThat(Glide.with(sibling).isPaused()).isFalse();
        });
  }

  @Test
  public void pauseRequestsRecursive_onFragment_withSibling_doesNotPauseSibling() {
    withActivityAndTwoFragmentSiblings(
        activity -> {
          Fragment fragment = getFragment(activity);
          Fragment sibling = getSiblingFragment(activity);

          Glide.with(fragment).pauseAllRequestsRecursive();

          assertThat(Glide.with(sibling).isPaused()).isFalse();
        });
  }

  @Test
  public void resumeRequestsRecursive_onFragment_withSibling_doesNotResumeSibling() {
    withActivityAndTwoFragmentSiblings(
        activity -> {
          Fragment fragment = getFragment(activity);
          Fragment sibling = getSiblingFragment(activity);

          Glide.with(fragment).pauseAllRequestsRecursive();
          Glide.with(sibling).pauseAllRequests();
          Glide.with(fragment).resumeRequestsRecursive();

          assertThat(Glide.with(sibling).isPaused()).isTrue();
        });
  }

  // We need to create the RequestManager first, or else it will start in the paused state.
  // TODO(judds): If the parent is explicitly paused, any children added after it's paused should
  //  probably default to paused when it's created?
  private void initRequestManagers(FragmentActivity activity, Fragment... fragments) {
    Glide.with(activity);
    for (Fragment fragment : fragments) {
      Glide.with(fragment);
    }
  }

  /** Creates the tree: Activity - Fragment - Fragment */
  private void withActivityAndTwoFragmentSiblings(
      ActivityAction<DefaultFragmentActivity> assertion) {
    setupAndRunActivityAction(
        activity -> {
          Fragment parentFragment = createAndAddFragment(activity, FRAGMENT_TAG);
          Fragment siblingFragment = createAndAddFragment(activity, FRAGMENT_SIBLING_TAG);
          initRequestManagers(activity, parentFragment, siblingFragment);
        },
        assertion);
  }

  /** Creates the tree: Activity - Fragment - Child Fragment */
  private void withActivityFragmentAndChildFragment(
      ActivityAction<DefaultFragmentActivity> assertion) {
    setupAndRunActivityAction(
        activity -> {
          Fragment parentFragment = createAndAddFragment(activity, FRAGMENT_TAG);
          Fragment childFragment = createAndAddFragment(parentFragment, CHILD_FRAGMENT_TAG);
          initRequestManagers(activity, parentFragment, childFragment);
        },
        assertion);
  }

  private void setupAndRunActivityAction(
      ActivityAction<DefaultFragmentActivity> setup,
      ActivityAction<DefaultFragmentActivity> assertion) {
    scenario.moveToState(State.RESUMED);
    // Using one onActivity call to do the test setup and another to assert gives the framework
    // and Glide's fragment management code (onAttach in particular) the opportunity to run before
    // our
    // assertions take place.
    scenario.onActivity(setup);
    scenario.onActivity(assertion);
  }

  private Fragment getFragment(FragmentActivity activity) {
    return getFragment(activity, FRAGMENT_TAG);
  }

  private Fragment getSiblingFragment(FragmentActivity activity) {
    return getFragment(activity, FRAGMENT_SIBLING_TAG);
  }

  private Fragment getChildFragment(FragmentActivity activity) {
    return getFragment(getFragment(activity).getChildFragmentManager(), CHILD_FRAGMENT_TAG);
  }

  private Fragment getFragment(FragmentActivity activity, String tag) {
    return getFragment(activity.getSupportFragmentManager(), tag);
  }

  private Fragment getFragment(FragmentManager manager, String tag) {
    return manager.findFragmentByTag(tag);
  }

  private Fragment createAndAddFragment(FragmentActivity parent, String tag) {
    return createAndAddFragment(parent.getSupportFragmentManager(), tag);
  }

  private Fragment createAndAddFragment(Fragment fragment, String tag) {
    return createAndAddFragment(fragment.getChildFragmentManager(), tag);
  }

  private Fragment createAndAddFragment(FragmentManager manager, String tag) {
    Fragment result = new EmptyContainerFragment();
    manager.beginTransaction().add(R.id.container, result, tag).commitNowAllowingStateLoss();
    return result;
  }

  public static final class EmptyContainerFragment extends Fragment {
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
      return inflater.inflate(
          R.layout.default_fragment_activity, container, /* attachToRoot= */ false);
    }
  }
}
