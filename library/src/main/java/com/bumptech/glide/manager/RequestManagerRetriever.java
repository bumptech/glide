package com.bumptech.glide.manager;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class RequestManagerRetriever {
    static final String TAG = "com.bumptech.glide.manager";

    public static RequestManager get(FragmentActivity activity) {
        if (activity.isDestroyed()) {
            throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
        }
        FragmentManager fm = activity.getSupportFragmentManager();
        return supportFragmentGet(fm);
    }

    public static RequestManager get(Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        }
        if (fragment.isDetached()) {
            throw new IllegalArgumentException("You cannot start a load on a detached fragment");
        }
        FragmentManager fm = fragment.getChildFragmentManager();
        return supportFragmentGet(fm);
    }

    public static RequestManager get(Activity activity) {
        if (activity.isDestroyed()) {
            throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
        }
        android.app.FragmentManager fm = activity.getFragmentManager();
        return fragmentGet(fm);
    }

    public static RequestManager get(android.app.Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        }
        if (fragment.isDetached()) {
            throw new IllegalArgumentException("You cannot start a load on a detached fragment");
        }
        android.app.FragmentManager fm = fragment.getChildFragmentManager();
        return fragmentGet(fm);
    }

    static RequestManager fragmentGet(android.app.FragmentManager fm) {
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
            requestManager = new LifecycleRequestManager();
            current.setRequestManager(requestManager);
        }
        return requestManager;

    }

    static RequestManager supportFragmentGet(FragmentManager fm) {
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
            requestManager = new LifecycleRequestManager();
            current.setRequestManager(requestManager);
        }
        return requestManager;
    }
}
