package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ActivityScenario.ActivityAction;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.LifecycleListener;
import com.bumptech.glide.manager.RequestManagerFragment;
import com.bumptech.glide.manager.RequestManagerTreeNode;
import com.bumptech.glide.manager.SupportRequestManagerFragment;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.GlideWithAsDifferentSupertypesActivity;
import com.bumptech.glide.test.GlideWithBeforeSuperOnCreateActivity;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.ResourceIds.raw;
import com.bumptech.glide.test.TearDownGlide;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class RequestManagerTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  @Mock private RequestManagerTreeNode treeNode;

  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private RequestManager requestManager;
  private Context context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();
    Glide glide = Glide.get(context);
    requestManager =
        new RequestManager(
            glide,
            new Lifecycle() {
              @Override
              public void addListener(@NonNull LifecycleListener listener) {
                listener.onStart();
              }

              @Override
              public void removeListener(@NonNull LifecycleListener listener) {
                // Do nothing.
              }
            },
            treeNode,
            context);
  }

  /** Tests #2262. */
  @Test
  public void clear_withNonOwningRequestManager_afterOwningManagerIsDestroyed_doesNotThrow() {
    // First destroy our Fragment/Activity RequestManager.
    requestManager.onDestroy();

    final ImageView imageView = new ImageView(context);
    imageView.measure(100, 100);
    imageView.layout(0, 0, 100, 100);
    // Then start a new load with our now destroyed RequestManager.
    concurrency.loadOnMainThread(requestManager.load(ResourceIds.raw.canonical), imageView);

    // Finally clear our new load with any RequestManager other than the one we used to start it.
    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.with(context).clear(imageView);
          }
        });
  }

  /** Tests b/69361054. */
  @Test
  public void clear_withNonOwningRequestManager_onBackgroundThread_doesNotThrow() {
    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            requestManager.onDestroy();
          }
        });

    final Target<Drawable> target = concurrency.wait(requestManager.load(raw.canonical).submit());

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.with(context).clear(target);
          }
        });
  }

  @Test
  public void with_beforeActivitySuperOnCreate_onlyAddsOneRequestManagerFragment() {
    ActivityScenario<GlideWithBeforeSuperOnCreateActivity> scenario =
        ActivityScenario.launch(GlideWithBeforeSuperOnCreateActivity.class);
    scenario.moveToState(State.RESUMED);
    scenario.onActivity(
        new ActivityAction<GlideWithBeforeSuperOnCreateActivity>() {
          @Override
          public void perform(GlideWithBeforeSuperOnCreateActivity activity) {
            List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
            List<Fragment> glideFragments = new ArrayList<>();
            for (Fragment fragment : fragments) {
              if (fragment instanceof SupportRequestManagerFragment) {
                glideFragments.add(fragment);
              }
            }
            // Ideally this would be exactly 1, but it's a bit tricky to implement. For now we're
            // content making sure that we're not adding multiple fragments.
            assertThat(glideFragments.size()).isAtMost(1);
          }
        });
    scenario.onActivity(
        new ActivityAction<GlideWithBeforeSuperOnCreateActivity>() {
          @Override
          public void perform(final GlideWithBeforeSuperOnCreateActivity activity) {
            new Handler(Looper.getMainLooper())
                .post(
                    new Runnable() {
                      @Override
                      public void run() {
                        Glide.with(activity);
                      }
                    });
          }
        });
    scenario.onActivity(
        new ActivityAction<GlideWithBeforeSuperOnCreateActivity>() {
          @Override
          public void perform(GlideWithBeforeSuperOnCreateActivity activity) {
            List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
            List<Fragment> glideFragments = new ArrayList<>();
            for (Fragment fragment : fragments) {
              if (fragment instanceof SupportRequestManagerFragment) {
                glideFragments.add(fragment);
              }
            }
            // Now that we've called Glide.with() after commitAllowingStateLoss will actually add
            // the
            // fragment, ie after onCreate, we can expect exactly one Fragment instance.
            assertThat(glideFragments.size()).isEqualTo(1);
          }
        });
  }

  @Test
  public void with_asDifferentSuperTypes_doesNotAddMultipleFragments() {
    ActivityScenario<GlideWithAsDifferentSupertypesActivity> scenario =
        ActivityScenario.launch(GlideWithAsDifferentSupertypesActivity.class);
    scenario.moveToState(State.RESUMED);
    scenario.onActivity(
        new ActivityAction<GlideWithAsDifferentSupertypesActivity>() {
          @Override
          public void perform(GlideWithAsDifferentSupertypesActivity activity) {
            Iterable<SupportRequestManagerFragment> glideSupportFragments =
                Iterables.filter(
                    activity.getSupportFragmentManager().getFragments(),
                    SupportRequestManagerFragment.class);
            Iterable<RequestManagerFragment> normalFragments =
                Iterables.filter(
                    getAllFragments(activity.getFragmentManager()), RequestManagerFragment.class);
            assertThat(normalFragments).hasSize(0);
            assertThat(glideSupportFragments).hasSize(1);
          }
        });
  }

  private List<android.app.Fragment> getAllFragments(android.app.FragmentManager fragmentManager) {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ? fragmentManager.getFragments()
        : getAllFragmentsPreO(fragmentManager);
  }

  // Hacks based on the implementation of FragmentManagerImpl in the non-support libraries that
  // allow us to iterate over and retrieve all active Fragments in a FragmentManager.
  private static final String FRAGMENT_INDEX_KEY = "key";

  private List<android.app.Fragment> getAllFragmentsPreO(
      android.app.FragmentManager fragmentManager) {
    Bundle tempBundle = new Bundle();
    int index = 0;
    List<android.app.Fragment> result = new ArrayList<>();
    while (true) {
      tempBundle.putInt(FRAGMENT_INDEX_KEY, index++);
      android.app.Fragment fragment = null;
      try {
        fragment = fragmentManager.getFragment(tempBundle, FRAGMENT_INDEX_KEY);
      } catch (Exception e) {
        // This generates log spam from FragmentManager anyway.
      }
      if (fragment == null) {
        break;
      }
      result.add(fragment);
    }
    return result;
  }
}
