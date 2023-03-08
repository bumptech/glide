package com.bumptech.glide.manager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.HardwareConfigState;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;
import java.util.Collection;
import java.util.Map;

/**
 * A collection of static methods for creating new {@link com.bumptech.glide.RequestManager}s or
 * retrieving existing ones from activities and fragment.
 */
public class RequestManagerRetriever implements Handler.Callback {
  @VisibleForTesting static final String FRAGMENT_TAG = "com.bumptech.glide.manager";
  /** The top application level RequestManager. */
  private volatile RequestManager applicationManager;

  private final RequestManagerFactory factory;

  // Objects used to find Fragments and Activities containing views.
  private final ArrayMap<View, Fragment> tempViewToSupportFragment = new ArrayMap<>();
  // This is really misplaced here, but to put it anywhere else means duplicating all of the
  // Fragment/Activity extraction logic that already exists here. It's gross, but less likely to
  // break.
  private final FrameWaiter frameWaiter;
  private final LifecycleRequestManagerRetriever lifecycleRequestManagerRetriever;

  public RequestManagerRetriever(@Nullable RequestManagerFactory factory) {
    this.factory = factory != null ? factory : DEFAULT_FACTORY;
    lifecycleRequestManagerRetriever = new LifecycleRequestManagerRetriever(this.factory);
    frameWaiter = buildFrameWaiter();
  }

  private static FrameWaiter buildFrameWaiter() {
    if (!HardwareConfigState.HARDWARE_BITMAPS_SUPPORTED
        || !HardwareConfigState.BLOCK_HARDWARE_BITMAPS_WHEN_GL_CONTEXT_MIGHT_NOT_BE_INITIALIZED) {
      return new DoNothingFirstFrameWaiter();
    }
    return new FirstFrameWaiter();
  }

  @NonNull
  private RequestManager getApplicationManager(@NonNull Context context) {
    // Either an application context or we're on a background thread.
    if (applicationManager == null) {
      synchronized (this) {
        if (applicationManager == null) {
          // Normally pause/resume is taken care of by the fragment we add to the fragment or
          // activity. However, in this case since the manager attached to the application will not
          // receive lifecycle events, we must force the manager to start resumed using
          // ApplicationLifecycle.

          // TODO(b/27524013): Factor out this Glide.get() call.
          Glide glide = Glide.get(context.getApplicationContext());
          applicationManager =
              factory.build(
                  glide,
                  new ApplicationLifecycle(),
                  new EmptyRequestManagerTreeNode(),
                  context.getApplicationContext());
        }
      }
    }

    return applicationManager;
  }

  @NonNull
  public RequestManager get(@NonNull Context context) {
    if (context == null) {
      throw new IllegalArgumentException("You cannot start a load on a null Context");
    } else if (Util.isOnMainThread() && !(context instanceof Application)) {
      if (context instanceof FragmentActivity) {
        return get((FragmentActivity) context);
      } else if (context instanceof ContextWrapper
          // Only unwrap a ContextWrapper if the baseContext has a non-null application context.
          // Context#createPackageContext may return a Context without an Application instance,
          // in which case a ContextWrapper may be used to attach one.
          && ((ContextWrapper) context).getBaseContext().getApplicationContext() != null) {
        return get(((ContextWrapper) context).getBaseContext());
      }
    }

    return getApplicationManager(context);
  }

  @NonNull
  public RequestManager get(@NonNull FragmentActivity activity) {
    if (Util.isOnBackgroundThread()) {
      return get(activity.getApplicationContext());
    }
    assertNotDestroyed(activity);
    frameWaiter.registerSelf(activity);
    boolean isActivityVisible = isActivityVisible(activity);
    Glide glide = Glide.get(activity.getApplicationContext());
    return lifecycleRequestManagerRetriever.getOrCreate(
        activity,
        glide,
        activity.getLifecycle(),
        activity.getSupportFragmentManager(),
        isActivityVisible);
  }

  @NonNull
  public RequestManager get(@NonNull Fragment fragment) {
    Preconditions.checkNotNull(
        fragment.getContext(),
        "You cannot start a load on a fragment before it is attached or after it is destroyed");
    if (Util.isOnBackgroundThread()) {
      return get(fragment.getContext().getApplicationContext());
    }
    // In some unusual cases, it's possible to have a Fragment not hosted by an activity. There's
    // not all that much we can do here. Most apps will be started with a standard activity. If
    // we manage not to register the first frame waiter for a while, the consequences are not
    // catastrophic, we'll just use some extra memory.
    if (fragment.getActivity() != null) {
      frameWaiter.registerSelf(fragment.getActivity());
    }
    FragmentManager fm = fragment.getChildFragmentManager();
    Context context = fragment.getContext();
    Glide glide = Glide.get(context.getApplicationContext());
    return lifecycleRequestManagerRetriever.getOrCreate(
        context, glide, fragment.getLifecycle(), fm, fragment.isVisible());
  }

  /**
   * @deprecated This is identical to calling {@link #get(Context)} with the application context.
   *     Use androidx Activities instead (ie {@link FragmentActivity}, or {@link
   *     androidx.appcompat.app.AppCompatActivity}).
   */
  @Deprecated
  @NonNull
  public RequestManager get(@NonNull Activity activity) {
    return get(activity.getApplicationContext());
  }

  @NonNull
  public RequestManager get(@NonNull View view) {
    if (Util.isOnBackgroundThread()) {
      return get(view.getContext().getApplicationContext());
    }

    Preconditions.checkNotNull(view);
    Preconditions.checkNotNull(
        view.getContext(), "Unable to obtain a request manager for a view without a Context");
    Activity activity = findActivity(view.getContext());
    // The view might be somewhere else, like a service.
    if (activity == null) {
      return get(view.getContext().getApplicationContext());
    }

    // Support Fragments.
    // Although the user might have non-support Fragments attached to FragmentActivity, searching
    // for non-support Fragments is so expensive pre O and that should be rare enough that we
    // prefer to just fall back to the Activity directly.
    if (activity instanceof FragmentActivity) {
      Fragment fragment = findSupportFragment(view, (FragmentActivity) activity);
      return fragment != null ? get(fragment) : get((FragmentActivity) activity);
    }

    // Standard Fragments.
    return get(view.getContext().getApplicationContext());
  }

  private static void findAllSupportFragmentsWithViews(
      @Nullable Collection<Fragment> topLevelFragments, @NonNull Map<View, Fragment> result) {
    if (topLevelFragments == null) {
      return;
    }
    for (Fragment fragment : topLevelFragments) {
      // getFragment()s in the support FragmentManager may contain null values, see #1991.
      if (fragment == null || fragment.getView() == null) {
        continue;
      }
      result.put(fragment.getView(), fragment);
      findAllSupportFragmentsWithViews(fragment.getChildFragmentManager().getFragments(), result);
    }
  }

  @Nullable
  private Fragment findSupportFragment(@NonNull View target, @NonNull FragmentActivity activity) {
    tempViewToSupportFragment.clear();
    findAllSupportFragmentsWithViews(
        activity.getSupportFragmentManager().getFragments(), tempViewToSupportFragment);
    Fragment result = null;
    View activityRoot = activity.findViewById(android.R.id.content);
    View current = target;
    while (!current.equals(activityRoot)) {
      result = tempViewToSupportFragment.get(current);
      if (result != null) {
        break;
      }
      if (current.getParent() instanceof View) {
        current = (View) current.getParent();
      } else {
        break;
      }
    }

    tempViewToSupportFragment.clear();
    return result;
  }

  @Nullable
  private static Activity findActivity(@NonNull Context context) {
    if (context instanceof Activity) {
      return (Activity) context;
    } else if (context instanceof ContextWrapper) {
      return findActivity(((ContextWrapper) context).getBaseContext());
    } else {
      return null;
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private static void assertNotDestroyed(@NonNull Activity activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
      throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
    }
  }

  /**
   * @deprecated This is equivalent to calling {@link #get(Context)} with the application context.
   *     Use androidx fragments instead: {@link Fragment}.
   */
  @Deprecated
  @NonNull
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  public RequestManager get(@NonNull android.app.Fragment fragment) {
    if (fragment.getActivity() == null) {
      throw new IllegalArgumentException(
          "You cannot start a load on a fragment before it is attached");
    }
    return get(fragment.getActivity().getApplicationContext());
  }

  private static boolean isActivityVisible(Context context) {
    // This is a poor heuristic, but it's about all we have. We'd rather err on the side of visible
    // and start requests than on the side of invisible and ignore valid requests.
    Activity activity = findActivity(context);
    return activity == null || !activity.isFinishing();
  }

  /**
   * @deprecated This method is no longer called by Glide or provides any functionality and it will
   *     be removed in the future. Retained for now to preserve backwards compatibility.
   */
  @Deprecated
  @SuppressWarnings("PMD.CollapsibleIfStatements")
  @Override
  public boolean handleMessage(Message message) {
    return false;
  }

  /** Used internally to create {@link RequestManager}s. */
  public interface RequestManagerFactory {
    @NonNull
    RequestManager build(
        @NonNull Glide glide,
        @NonNull Lifecycle lifecycle,
        @NonNull RequestManagerTreeNode requestManagerTreeNode,
        @NonNull Context context);
  }

  private static final RequestManagerFactory DEFAULT_FACTORY =
      new RequestManagerFactory() {
        @NonNull
        @Override
        public RequestManager build(
            @NonNull Glide glide,
            @NonNull Lifecycle lifecycle,
            @NonNull RequestManagerTreeNode requestManagerTreeNode,
            @NonNull Context context) {
          return new RequestManager(glide, lifecycle, requestManagerTreeNode, context);
        }
      };
}
