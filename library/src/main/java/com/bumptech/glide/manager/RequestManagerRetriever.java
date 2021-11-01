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

  private static final int ID_REMOVE_FRAGMENT_MANAGER = 1;
  private static final int ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 2;

  // Hacks based on the implementation of FragmentManagerImpl in the non-support libraries that
  // allow us to iterate over and retrieve all active Fragments in a FragmentManager.
  private static final String FRAGMENT_INDEX_KEY = "key";

  /**
   * The top application level RequestManager.
   */
  private volatile RequestManager applicationManager;

  /**
   * Pending adds for RequestManagerFragments.
   */
  @SuppressWarnings("deprecation")
  @VisibleForTesting
  final Map<android.app.FragmentManager, RequestManagerFragment> pendingRequestManagerFragments =
      new HashMap<>();

  /**
   * Pending adds for SupportRequestManagerFragments.
   */
  @VisibleForTesting final Map<FragmentManager, SupportRequestManagerFragment> pendingSupportRequestManagerFragments =
      new HashMap<>();

  /**
   * Main thread handler to handle cleaning up pending fragment maps.
   */
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

  public RequestManagerRetriever(
      @Nullable RequestManagerFactory factory, GlideExperiments experiments) {
    this.factory = factory != null ? factory : DEFAULT_FACTORY;
    handler = new Handler(Looper.getMainLooper(), this /* Callback */);

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

  // TODO: Glide源码-Glide.with(context)--调用RequestManagerRetriever.get()获取RequestManager
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
    //TODO: 子线程或ApplicationContext，直接创建ApplicationRequestManager->new RequestManager(glide, lifecycle, requestManagerTreeNode, context)
    return getApplicationManager(context);
  }

  @NonNull
  // TODO: Glide源码-Glide.with(context)--调用RequestManagerRetriever.get()获取RequestManager
  //androidx
  public RequestManager get(@NonNull FragmentActivity activity) {
    if (Util.isOnBackgroundThread()) {
      return get(activity.getApplicationContext());
    } else {
      assertNotDestroyed(activity);
      frameWaiter.registerSelf(activity);
      FragmentManager fm = activity.getSupportFragmentManager();
      return supportFragmentGet(activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
    }
  }

  @NonNull
  public RequestManager get(@NonNull Fragment fragment) {
    Preconditions.checkNotNull(
        fragment.getContext(),
        "You cannot start a load on a fragment before it is attached or after it is destroyed");
    if (Util.isOnBackgroundThread()) {
      return get(fragment.getContext().getApplicationContext());
    } else {
      // In some unusual cases, it's possible to have a Fragment not hosted by an activity. There's
      // not all that much we can do here. Most apps will be started with a standard activity. If
      // we manage not to register the first frame waiter for a while, the consequences are not
      // catastrophic, we'll just use some extra memory.
      if (fragment.getActivity() != null) {
        frameWaiter.registerSelf(fragment.getActivity());
      }
      FragmentManager fm = fragment.getChildFragmentManager();
      return supportFragmentGet(fragment.getContext(), fm, fragment, fragment.isVisible());
    }
  }

  @SuppressWarnings("deprecation")
  // TODO: Glide源码-Glide.with(context)--调用RequestManagerRetriever.get()获取RequestManager
  //app包
  @NonNull
  public RequestManager get(@NonNull Activity activity) {
    //非主线程，传入applicationContext
    if (Util.isOnBackgroundThread()) {
      return get(activity.getApplicationContext());
    } else if (activity instanceof FragmentActivity) {
      return get((FragmentActivity) activity);
    } else {
      assertNotDestroyed(activity);
      frameWaiter.registerSelf(activity);
      android.app.FragmentManager fm = activity.getFragmentManager();
      // 创建空白fragment绑定页面监听生命周期变化，
      // 将requestManager传给fragment用来接收生命周期变化，
      // 返回requestManager
      return fragmentGet(activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
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

  @SuppressWarnings("deprecation")
  @Deprecated
  @NonNull
  RequestManagerFragment getRequestManagerFragment(Activity activity) {
    return getRequestManagerFragment(activity.getFragmentManager(), /*parentHint=*/ null);
  }

  @SuppressWarnings("deprecation")
  @NonNull
  // TODO: Glide源码-Glide.with(context)--创建空白fragment,绑定页面
  //这里是app包下的Fragment
  private RequestManagerFragment getRequestManagerFragment(
      @NonNull final android.app.FragmentManager fm, @Nullable android.app.Fragment parentHint) {
    //先根据tag获取，如果已经绑定过
    RequestManagerFragment current = (RequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
    if (current == null) {
      //先从hashmap中获取，缓存作用，避免重复创建fragment
      current = pendingRequestManagerFragments.get(fm);
      if (current == null) {
        //创建fragment
        current = new RequestManagerFragment();
        //fragment中绑定fragmetn用--分支
        current.setParentFragmentHint(parentHint);
        //放入hashMap缓存中
        pendingRequestManagerFragments.put(fm, current);
        //绑定页面
        fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        //发送handler消息，因为绑定fragment是基于handler消息的，
        //这里发送消息，一定会在绑定fragment消息之后，消费这个消息时说明绑定过程已经结束。
        //所以hashMap防止重复创建就可以消除了，上面第一步直接可以从通过tag获取到了。
        handler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget();
      }
    }
    return current;
  }

  @SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
  @Deprecated
  // TODO: Glide源码-Glide.with(context)--创建空白fragment,创建requestManager,传给fragment，返回requestManager
  @NonNull
  private RequestManager fragmentGet(
      @NonNull Context context,
      @NonNull android.app.FragmentManager fm,
      @Nullable android.app.Fragment parentHint,
      boolean isParentVisible) {
    //创建fragment
    RequestManagerFragment current = getRequestManagerFragment(fm, parentHint);
    //先获取frag中的requestManager,因为这里的frag有可能是通过byTag获取已经绑定过的frag.
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      Glide glide = Glide.get(context);
      //通过建造者创建requestManager，传入glide和frag在创建时创建的lifecycle，用于生命周期变化通知。
      //调用requestManager构造方法，将rm作为LifecycleListener,传给frag的lifecycle中->
      requestManager =
          factory.build(
              glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
      // This is a bit of hack, we're going to start the RequestManager, but not the
      // corresponding Lifecycle. It's safe to start the RequestManager, but starting the
      // Lifecycle might trigger memory leaks. See b/154405040
      //activity可见的，则调用rm的onStart方法
      //如果已经有执行的请求就开始
      if (isParentVisible) {
        requestManager.onStart();
      }
      //将rm设置给fragment
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }

  @NonNull
  SupportRequestManagerFragment getSupportRequestManagerFragment(FragmentManager fragmentManager) {
    return getSupportRequestManagerFragment(fragmentManager, /*parentHint=*/ null);
  }

  private static boolean isActivityVisible(Context context) {
    // This is a poor heuristic, but it's about all we have. We'd rather err on the side of visible
    // and start requests than on the side of invisible and ignore valid requests.
    Activity activity = findActivity(context);
    return activity == null || !activity.isFinishing();
  }

  // TODO: Glide源码-Glide.with(context)--创建空白fragment,绑定页面
  //这里是androidX包下的Fragment
  @NonNull
  private SupportRequestManagerFragment getSupportRequestManagerFragment(
      @NonNull final FragmentManager fm, @Nullable Fragment parentHint) {
    SupportRequestManagerFragment current =
        (SupportRequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
    if (current == null) {
      current = pendingSupportRequestManagerFragments.get(fm);
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

  @NonNull
  // TODO: Glide源码-Glide.with(context)--创建空白fragment,创建requestManager,传给fragment，返回requestManager
  private RequestManager supportFragmentGet(
      @NonNull Context context,
      @NonNull FragmentManager fm,
      @Nullable Fragment parentHint,
      boolean isParentVisible) {
    //创建空白fragment
    SupportRequestManagerFragment current = getSupportRequestManagerFragment(fm, parentHint);
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
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

  @Override
  public boolean handleMessage(Message message) {
    boolean handled = true;
    Object removed = null;
    Object key = null;
    switch (message.what) {
      case ID_REMOVE_FRAGMENT_MANAGER:
        android.app.FragmentManager fm = (android.app.FragmentManager) message.obj;
        key = fm;
        removed = pendingRequestManagerFragments.remove(fm);
        break;
      case ID_REMOVE_SUPPORT_FRAGMENT_MANAGER:
        FragmentManager supportFm = (FragmentManager) message.obj;
        key = supportFm;
        removed = pendingSupportRequestManagerFragments.remove(supportFm);
        break;
      default:
        handled = false;
        break;
    }
    if (handled && removed == null && Log.isLoggable(TAG, Log.WARN)) {
      Log.w(TAG, "Failed to remove expected request manager fragment, manager: " + key);
    }
    return handled;
  }

  /**
   * Used internally to create {@link RequestManager}s.
   */
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
          //调用构造方法
          return new RequestManager(glide, lifecycle, requestManagerTreeNode, context);
        }
      };
}
