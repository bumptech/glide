package com.bumptech.glide.manager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder.WaitForFramesAfterTrimMemory;
import com.bumptech.glide.GlideExperiments;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.HardwareConfigState;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of static methods for creating new {@link com.bumptech.glide.RequestManager}s or
 * retrieving existing ones from activities and fragment.
 */
public class RequestManagerRetriever implements Handler.Callback {
  @VisibleForTesting static final String FRAGMENT_TAG = "com.bumptech.glide.manager";
  private static final String TAG = "RMRetriever";

  // Indicates that we've tried to add a RequestManagerFragment twice previously and is used as a
  // signal to give up and tear down the fragment.
  private static final int HAS_ATTEMPTED_TO_ADD_FRAGMENT_TWICE = 1;

  private static final int ID_REMOVE_FRAGMENT_MANAGER = 1;
  private static final int ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 2;

  // Hacks based on the implementation of FragmentManagerImpl in the non-support libraries that
  // allow us to iterate over and retrieve all active Fragments in a FragmentManager.
  private static final String FRAGMENT_INDEX_KEY = "key";

  /** The top application level RequestManager. */
  private volatile RequestManager applicationManager;

  /** Pending adds for RequestManagerFragments. */
  @SuppressWarnings("deprecation")
  @VisibleForTesting
  final Map<android.app.FragmentManager, RequestManagerFragment> pendingRequestManagerFragments =
      new HashMap<>();

  /** Pending adds for SupportRequestManagerFragments. */
  @VisibleForTesting
  final Map<FragmentManager, SupportRequestManagerFragment> pendingSupportRequestManagerFragments =
      new HashMap<>();

  /** Main thread handler to handle cleaning up pending fragment maps. */
  private final Handler handler;

  private final RequestManagerFactory factory;

  // Objects used to find Fragments and Activities containing views.
  private final ArrayMap<View, Fragment> tempViewToSupportFragment = new ArrayMap<>();
  private final ArrayMap<View, android.app.Fragment> tempViewToFragment = new ArrayMap<>();
  private final Bundle tempBundle = new Bundle();
  // This is really misplaced here, but to put it anywhere else means duplicating all of the
  // Fragment/Activity extraction logic that already exists here. It's gross, but less likely to
  // break.
  private final FrameWaiter frameWaiter;
  private final LifecycleRequestManagerRetriever lifecycleRequestManagerRetriever;

  public RequestManagerRetriever(
      @Nullable RequestManagerFactory factory, GlideExperiments experiments) {
    this.factory = factory != null ? factory : DEFAULT_FACTORY;
    handler = new Handler(Looper.getMainLooper(), this /* Callback */);
    lifecycleRequestManagerRetriever = new LifecycleRequestManagerRetriever(this.factory);
    frameWaiter = buildFrameWaiter(experiments);
  }

  private static FrameWaiter buildFrameWaiter(GlideExperiments experiments) {
    if (!HardwareConfigState.HARDWARE_BITMAPS_SUPPORTED
        || !HardwareConfigState.BLOCK_HARDWARE_BITMAPS_WHEN_GL_CONTEXT_MIGHT_NOT_BE_INITIALIZED) {
      return new DoNothingFirstFrameWaiter();
    }
    return experiments.isEnabled(WaitForFramesAfterTrimMemory.class)
        ? new FirstFrameAndAfterTrimMemoryWaiter()
        : new FirstFrameWaiter();
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
      } else if (context instanceof Activity) {
        return get((Activity) context);
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
   * @deprecated Use androidx Activities instead (ie {@link FragmentActivity}, or {@link
   *     androidx.appcompat.app.AppCompatActivity}).
   */
  @Deprecated
  @SuppressWarnings("deprecation")
  @NonNull
  public RequestManager get(@NonNull Activity activity) {
    if (Util.isOnBackgroundThread()) {
      return get(activity.getApplicationContext());
    } else if (activity instanceof FragmentActivity) {
      return get((FragmentActivity) activity);
    } else {
      assertNotDestroyed(activity);
      frameWaiter.registerSelf(activity);
      android.app.FragmentManager fm = activity.getFragmentManager();
      return fragmentGet(activity, fm, /* parentHint= */ null, isActivityVisible(activity));
    }
  }

  @SuppressWarnings("deprecation")
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
    android.app.Fragment fragment = findFragment(view, activity);
    if (fragment == null) {
      return get(activity);
    }
    return get(fragment);
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

  @SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
  @Deprecated
  @Nullable
  private android.app.Fragment findFragment(@NonNull View target, @NonNull Activity activity) {
    tempViewToFragment.clear();
    findAllFragmentsWithViews(activity.getFragmentManager(), tempViewToFragment);

    android.app.Fragment result = null;

    View activityRoot = activity.findViewById(android.R.id.content);
    View current = target;
    while (!current.equals(activityRoot)) {
      result = tempViewToFragment.get(current);
      if (result != null) {
        break;
      }
      if (current.getParent() instanceof View) {
        current = (View) current.getParent();
      } else {
        break;
      }
    }
    tempViewToFragment.clear();
    return result;
  }

  // TODO: Consider using an accessor class in the support library package to more directly retrieve
  // non-support Fragments.
  @SuppressWarnings("deprecation")
  @Deprecated
  @TargetApi(Build.VERSION_CODES.O)
  private void findAllFragmentsWithViews(
      @NonNull android.app.FragmentManager fragmentManager,
      @NonNull ArrayMap<View, android.app.Fragment> result) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      for (android.app.Fragment fragment : fragmentManager.getFragments()) {
        if (fragment.getView() != null) {
          result.put(fragment.getView(), fragment);
          findAllFragmentsWithViews(fragment.getChildFragmentManager(), result);
        }
      }
    } else {
      findAllFragmentsWithViewsPreO(fragmentManager, result);
    }
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  private void findAllFragmentsWithViewsPreO(
      @NonNull android.app.FragmentManager fragmentManager,
      @NonNull ArrayMap<View, android.app.Fragment> result) {
    int index = 0;
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
      if (fragment.getView() != null) {
        result.put(fragment.getView(), fragment);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
          findAllFragmentsWithViews(fragment.getChildFragmentManager(), result);
        }
      }
    }
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
   * @deprecated Use androidx fragments instead: {@link Fragment}.
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  @NonNull
  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  public RequestManager get(@NonNull android.app.Fragment fragment) {
    if (fragment.getActivity() == null) {
      throw new IllegalArgumentException(
          "You cannot start a load on a fragment before it is attached");
    }
    if (Util.isOnBackgroundThread() || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return get(fragment.getActivity().getApplicationContext());
    } else {
      // In some unusual cases, it's possible to have a Fragment not hosted by an activity. There's
      // not all that much we can do here. Most apps will be started with a standard activity. If
      // we manage not to register the first frame waiter for a while, the consequences are not
      // catastrophic, we'll just use some extra memory.
      if (fragment.getActivity() != null) {
        frameWaiter.registerSelf(fragment.getActivity());
      }
      android.app.FragmentManager fm = fragment.getChildFragmentManager();
      return fragmentGet(fragment.getActivity(), fm, fragment, fragment.isVisible());
    }
  }

  /**
   * @deprecated Use androidx activities like {@link FragmentActivity} or {@link
   *     androidx.appcompat.app.AppCompatActivity} instead.
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  @NonNull
  RequestManagerFragment getRequestManagerFragment(Activity activity) {
    return getRequestManagerFragment(activity.getFragmentManager(), /* parentHint= */ null);
  }

  @SuppressWarnings("deprecation")
  @NonNull
  private RequestManagerFragment getRequestManagerFragment(
      @NonNull final android.app.FragmentManager fm, @Nullable android.app.Fragment parentHint) {
    // If we have a pending Fragment, we need to continue to use the pending Fragment. Otherwise
    // there's a race where an old Fragment could be added and retrieved here before our logic to
    // add our pending Fragment notices. That can then result in both the pending Fragmeng and the
    // old Fragment having requests running for them, which is impossible to safely unwind.
    RequestManagerFragment current = pendingRequestManagerFragments.get(fm);
    if (current == null) {
      current = (RequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
      if (current == null) {
        current = new RequestManagerFragment();
        current.setParentFragmentHint(parentHint);
        pendingRequestManagerFragments.put(fm, current);
        fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        handler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget();
      }
    }
    return current;
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @NonNull
  private RequestManager fragmentGet(
      @NonNull Context context,
      @NonNull android.app.FragmentManager fm,
      @Nullable android.app.Fragment parentHint,
      boolean isParentVisible) {
    RequestManagerFragment current = getRequestManagerFragment(fm, parentHint);
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      // TODO(b/27524013): Factor out this Glide.get() call.
      Glide glide = Glide.get(context);
      requestManager =
          factory.build(
              glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
      // This is a bit of hack, we're going to start the RequestManager, but not the
      // corresponding Lifecycle. It's safe to start the RequestManager, but starting the
      // Lifecycle might trigger memory leaks. See b/154405040
      if (isParentVisible) {
        requestManager.onStart();
      }
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }

  @NonNull
  SupportRequestManagerFragment getSupportRequestManagerFragment(FragmentManager fragmentManager) {
    return getSupportRequestManagerFragment(fragmentManager, /* parentHint= */ null);
  }

  private static boolean isActivityVisible(Context context) {
    // This is a poor heuristic, but it's about all we have. We'd rather err on the side of visible
    // and start requests than on the side of invisible and ignore valid requests.
    Activity activity = findActivity(context);
    return activity == null || !activity.isFinishing();
  }

  @NonNull
  private SupportRequestManagerFragment getSupportRequestManagerFragment(
      @NonNull final FragmentManager fm, @Nullable Fragment parentHint) {
    // If we have a pending Fragment, we need to continue to use the pending Fragment. Otherwise
    // there's a race where an old Fragment could be added and retrieved here before our logic to
    // add our pending Fragment notices. That can then result in both the pending Fragmeng and the
    // old Fragment having requests running for them, which is impossible to safely unwind.
    SupportRequestManagerFragment current = pendingSupportRequestManagerFragments.get(fm);
    if (current == null) {
      current = (SupportRequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
      if (current == null) {
        current = new SupportRequestManagerFragment();
        current.setParentFragmentHint(parentHint);
        pendingSupportRequestManagerFragments.put(fm, current);
        fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        handler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
      }
    }
    return current;
  }

  // We care about the instance specifically.
  @SuppressWarnings({"ReferenceEquality", "PMD.CompareObjectsWithEquals"})
  private boolean verifyOurFragmentWasAddedOrCantBeAdded(
      android.app.FragmentManager fm, boolean hasAttemptedToAddFragmentTwice) {
    RequestManagerFragment newlyAddedRequestManagerFragment =
        pendingRequestManagerFragments.get(fm);

    RequestManagerFragment actualFragment =
        (RequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
    if (actualFragment == newlyAddedRequestManagerFragment) {
      return true;
    }

    if (actualFragment != null && actualFragment.getRequestManager() != null) {
      throw new IllegalStateException(
          "We've added two fragments with requests!"
              + " Old: "
              + actualFragment
              + " New: "
              + newlyAddedRequestManagerFragment);
    }

    // If our parent was destroyed, we're never going to be able to add our fragment, so we should
    // just clean it up and abort.
    // Similarly if we've already tried to add the fragment, waited a frame, then tried to add the
    // fragment a second time and still the fragment isn't present, we're unlikely to be able to do
    // so if we retry a third time. This is easy to reproduce in Robolectric by obtaining an
    // Activity but not creating it. If we continue to loop forever, we break tests and, if it
    // happens in the real world, might leak memory and waste a bunch of CPU/battery.
    if (hasAttemptedToAddFragmentTwice || fm.isDestroyed()) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        if (fm.isDestroyed()) {
          Log.w(TAG, "Parent was destroyed before our Fragment could be added");
        } else {
          Log.w(TAG, "Tried adding Fragment twice and failed twice, giving up!");
        }
      }
      newlyAddedRequestManagerFragment.getGlideLifecycle().onDestroy();
      return true;
    }

    // Otherwise we should make another attempt to commit the fragment and loop back again in the
    // next frame to verify.
    android.app.FragmentTransaction transaction =
        fm.beginTransaction().add(newlyAddedRequestManagerFragment, FRAGMENT_TAG);
    // If the Activity is re-created and a Glide request was started for that Activity prior to the
    // re-creation, then there will be an old RequestManagerFragment that is re-created as well.
    // Under normal circumstances we find and re-use that Fragment rather than creating a new one.
    // However, if the first Glide request for the re-created Activity occurs before the Activity is
    // created, then we will have been unable to find the old RequestManagerFragment and will have
    // created a new one instead. We don't want to keep adding new Fragments infinitely as the
    // Activity is re-created, so we need to pick one. If we pick the old Fragment, then we will
    // drop any requests that had been started after re-creation and are associated with the new
    // Fragment. So here we drop the old Fragment if it exists.
    if (actualFragment != null) {
      transaction.remove(actualFragment);
    }
    transaction.commitAllowingStateLoss();

    handler
        .obtainMessage(
            ID_REMOVE_FRAGMENT_MANAGER, HAS_ATTEMPTED_TO_ADD_FRAGMENT_TWICE, /* arg2= */ 0, fm)
        .sendToTarget();
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "We failed to add our Fragment the first time around, trying again...");
    }
    return false;
  }

  // We care about the instance specifically.
  @SuppressWarnings({"ReferenceEquality", "PMD.CompareObjectsWithEquals"})
  private boolean verifyOurSupportFragmentWasAddedOrCantBeAdded(
      FragmentManager supportFm, boolean hasAttemptedToAddFragmentTwice) {
    SupportRequestManagerFragment newlyAddedSupportRequestManagerFragment =
        pendingSupportRequestManagerFragments.get(supportFm);

    SupportRequestManagerFragment actualFragment =
        (SupportRequestManagerFragment) supportFm.findFragmentByTag(FRAGMENT_TAG);
    if (actualFragment == newlyAddedSupportRequestManagerFragment) {
      return true;
    }

    if (actualFragment != null && actualFragment.getRequestManager() != null) {
      throw new IllegalStateException(
          "We've added two fragments with requests!"
              + " Old: "
              + actualFragment
              + " New: "
              + newlyAddedSupportRequestManagerFragment);
    }
    // If our parent was destroyed, we're never going to be able to add our fragment, so we should
    // just clean it up and abort.
    // Similarly if we've already tried to add the fragment, waited a frame, then tried to add the
    // fragment a second time and still the fragment isn't present, we're unlikely to be able to do
    // so if we retry a third time. This is easy to reproduce in Robolectric by obtaining an
    // Activity but not creating it. If we continue to loop forever, we break tests and, if it
    // happens in the real world, might leak memory and waste a bunch of CPU/battery.
    if (hasAttemptedToAddFragmentTwice || supportFm.isDestroyed()) {
      if (supportFm.isDestroyed()) {
        if (Log.isLoggable(TAG, Log.WARN)) {
          Log.w(
              TAG,
              "Parent was destroyed before our Fragment could be added, all requests for the"
                  + " destroyed parent are cancelled");
        }
      } else {
        if (Log.isLoggable(TAG, Log.ERROR)) {
          Log.e(
              TAG,
              "ERROR: Tried adding Fragment twice and failed twice, giving up and cancelling all"
                  + " associated requests! This probably means you're starting loads in a unit test"
                  + " with an Activity that you haven't created and never create. If you're using"
                  + " Robolectric, create the Activity as part of your test setup");
        }
      }
      newlyAddedSupportRequestManagerFragment.getGlideLifecycle().onDestroy();
      return true;
    }

    // Otherwise we should make another attempt to commit the fragment and loop back again in the
    // next frame to verify.
    FragmentTransaction transaction =
        supportFm.beginTransaction().add(newlyAddedSupportRequestManagerFragment, FRAGMENT_TAG);

    // If the Activity is re-created and a Glide request was started for that Activity prior to the
    // re-creation, then there will be an old RequestManagerFragment that is re-created as well.
    // Under normal circumstances we find and re-use that Fragment rather than creating a new one.
    // However, if the first Glide request for the re-created Activity occurs before the Activity is
    // created, then we will have been unable to find the old RequestManagerFragment and will have
    // created a new one instead. We don't want to keep adding new Fragments infinitely as the
    // Activity is re-created, so we need to pick one. If we pick the old Fragment, then we will
    // drop any requests that had been started after re-creation and are associated with the new
    // Fragment. So here we drop the old Fragment if it exists.
    if (actualFragment != null) {
      transaction.remove(actualFragment);
    }
    transaction.commitNowAllowingStateLoss();

    handler
        .obtainMessage(
            ID_REMOVE_SUPPORT_FRAGMENT_MANAGER,
            HAS_ATTEMPTED_TO_ADD_FRAGMENT_TWICE,
            /* arg2= */ 0,
            supportFm)
        .sendToTarget();
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "We failed to add our Fragment the first time around, trying again...");
    }
    return false;
  }

  @SuppressWarnings("PMD.CollapsibleIfStatements")
  @Override
  public boolean handleMessage(Message message) {
    boolean handled = true;
    boolean attemptedRemoval = false;
    Object removed = null;
    Object key = null;
    boolean hasAttemptedBefore = message.arg1 == HAS_ATTEMPTED_TO_ADD_FRAGMENT_TWICE;
    switch (message.what) {
      case ID_REMOVE_FRAGMENT_MANAGER:
        android.app.FragmentManager fm = (android.app.FragmentManager) message.obj;
        if (verifyOurFragmentWasAddedOrCantBeAdded(fm, hasAttemptedBefore)) {
          attemptedRemoval = true;
          key = fm;
          removed = pendingRequestManagerFragments.remove(fm);
        }
        break;
      case ID_REMOVE_SUPPORT_FRAGMENT_MANAGER:
        FragmentManager supportFm = (FragmentManager) message.obj;
        if (verifyOurSupportFragmentWasAddedOrCantBeAdded(supportFm, hasAttemptedBefore)) {
          attemptedRemoval = true;
          key = supportFm;
          removed = pendingSupportRequestManagerFragments.remove(supportFm);
        }
        break;
      default:
        handled = false;
        break;
    }
    if (Log.isLoggable(TAG, Log.WARN)) {
      if (attemptedRemoval && removed == null) {
        Log.w(TAG, "Failed to remove expected request manager fragment, manager: " + key);
      }
    }
    return handled;
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
