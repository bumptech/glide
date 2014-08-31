package com.bumptech.glide.manager;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.util.Util;

/**
 * A collection of static methods for creating new {@link com.bumptech.glide.RequestManager}s or retrieving existing
 * ones from activities and fragment.
 */
public class RequestManagerRetriever {
    static final String TAG = "com.bumptech.glide.manager";
    private static RequestManager applicationManager;

    public static RequestManager get(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("You cannot start a load on a null Context");
        } else if (Util.isOnMainThread() && !(context instanceof Application)) {
            if (context instanceof FragmentActivity) {
                return get((FragmentActivity) context);
            } else if (context instanceof Activity) {
                return get((Activity) context);
            } else if (context instanceof ContextWrapper) {
                return get(((ContextWrapper) context).getBaseContext());
            }
        }

        // Either an application context or we're on a background thread.
        if (applicationManager == null) {
            synchronized (RequestManagerRetriever.class) {
                if (applicationManager == null) {
                    applicationManager = new RequestManager(context.getApplicationContext(), new Lifecycle());
                }
            }
        }
        return applicationManager;
    }

    @SuppressLint("NewApi")
    public static RequestManager get(FragmentActivity activity) {
        if (Util.isOnBackgroundThread()) {
            return get(activity.getApplicationContext());
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
                throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
            }
            FragmentManager fm = activity.getSupportFragmentManager();
            return supportFragmentGet(activity, fm);
        }
    }

    public static RequestManager get(Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        }
        if (Util.isOnBackgroundThread()) {
            return get(fragment.getActivity().getApplicationContext());
        } else {
            if (fragment.isDetached()) {
                throw new IllegalArgumentException("You cannot start a load on a detached fragment");
            }
            FragmentManager fm = fragment.getChildFragmentManager();
            return supportFragmentGet(fragment.getActivity(), fm);
        }
    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static RequestManager get(Activity activity) {
        if (Util.isOnBackgroundThread()) {
            return get(activity.getApplicationContext());
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
                throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
            }
            android.app.FragmentManager fm = activity.getFragmentManager();
            return fragmentGet(activity, fm);
        }
    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static RequestManager get(android.app.Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        }
        if (Util.isOnBackgroundThread()) {
            return get(fragment.getActivity().getApplicationContext());
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2 && fragment.isDetached()) {
                throw new IllegalArgumentException("You cannot start a load on a detached fragment");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                android.app.FragmentManager fm = fragment.getChildFragmentManager();
                return fragmentGet(fragment.getActivity(), fm);
            } else {
                return get(fragment.getActivity().getApplicationContext());
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
        RequestManager requestManager = current.getRequestManager();
        if (requestManager == null) {
            requestManager = new RequestManager(context, current.getLifecycle());
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
        RequestManager requestManager = current.getRequestManager();
        if (requestManager == null) {
            requestManager = new RequestManager(context, current.getLifecycle());
            current.setRequestManager(requestManager);
        }
        return requestManager;
    }
}
