package com.bumptech.glide.manager;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class RequestManagerRetriever {
    static final String TAG = "com.bumptech.glide.manager";
    private static final RequestManager NULL_MANAGER = new NullRequestManager();

    public static RequestManager get(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("You cannot start a load on a null Context");
        } else if (context instanceof FragmentActivity) {
            return get((FragmentActivity) context);
        } else if (context instanceof Activity) {
            return get((Activity) context);
        } else {
            return NULL_MANAGER;
        }
    }

    public static RequestManager get(FragmentActivity activity) {
        if (activity.isDestroyed()) {
            throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
        }
        FragmentManager fm = activity.getSupportFragmentManager();
        return supportFragmentGet(activity, fm);
    }

    public static RequestManager get(Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        }
        if (fragment.isDetached()) {
            throw new IllegalArgumentException("You cannot start a load on a detached fragment");
        }
        FragmentManager fm = fragment.getChildFragmentManager();
        return supportFragmentGet(fragment.getActivity(), fm);
    }

    public static RequestManager get(Activity activity) {
        if (activity.isDestroyed()) {
            throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
        }
        android.app.FragmentManager fm = activity.getFragmentManager();
        return fragmentGet(activity, fm);
    }

    public static RequestManager get(android.app.Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        }
        if (fragment.isDetached()) {
            throw new IllegalArgumentException("You cannot start a load on a detached fragment");
        }
        android.app.FragmentManager fm = fragment.getChildFragmentManager();
        return fragmentGet(fragment.getActivity(), fm);
    }

    static RequestManager fragmentGet(Context context, android.app.FragmentManager fm) {
        RequestManagerFragment current = (RequestManagerFragment) fm.findFragmentByTag(TAG);
        if (current == null) {
            current = new RequestManagerFragment();
            fm.beginTransaction().add(current, TAG).commitAllowingStateLoss();
            // Normally fragment transactions are posted to the main thread. Since we may start multiple requests within
            // a single synchronous call, we need to make sure that we only add a single fragment for the first call. To
            // do so, we use executePendingTransactions to skip the post and synchronously add the new fragment so that
            // the next synchronous request will retrieve it rather than creating a new one.
            fm.executePendingTransactions();
        }
        LifecycleRequestManager requestManager = current.getRequestManager();
        if (requestManager == null) {
            requestManager = new LifecycleRequestManager(context);
            current.setRequestManager(requestManager);
        }
        return requestManager;

    }

    static RequestManager supportFragmentGet(Context context, FragmentManager fm) {
        SupportRequestManagerFragment current = (SupportRequestManagerFragment) fm.findFragmentByTag(TAG);
        if (current == null) {
            current = new SupportRequestManagerFragment();
            fm.beginTransaction().add(current, TAG).commitAllowingStateLoss();
            // Normally fragment transactions are posted to the main thread. Since we may start multiple requests within
            // a single synchronous call, we need to make sure that we only add a single fragment for the first call. To
            // do so, we use executePendingTransactions to skip the post and synchronously add the new fragment so that
            // the next synchronous request will retrieve it rather than creating a new one.
            fm.executePendingTransactions();
        }
        LifecycleRequestManager requestManager = current.getRequestManager();
        if (requestManager == null) {
            requestManager = new LifecycleRequestManager(context);
            current.setRequestManager(requestManager);
        }
        return requestManager;
    }
}
