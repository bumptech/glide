package com.bumptech.glide.volley;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.bumptech.glide.load.engine.cache.DiskCache;

import static android.content.pm.PackageManager.NameNotFoundException;

public class RequestQueueWrapper {

    public static RequestQueue getRequestQueue(Context context) {
        return getRequestQueue(context, new NoCache());
    }

    public static RequestQueue getRequestQueue(Context context, DiskCache diskCache) {
        return getRequestQueue(context, new VolleyDiskCacheWrapper(diskCache));
    }

    public static RequestQueue getRequestQueue(Context context, Cache diskCache) {
        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (NameNotFoundException e) {
        }

        final HttpStack stack;
        if (Build.VERSION.SDK_INT >= 9) {
            stack = new HurlStack();
        } else {
            // Prior to Gingerbread, HttpUrlConnection was unreliable.
            // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
            stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
        }

        Network network = new BasicNetwork(stack);


        RequestQueue queue = new RequestQueue(diskCache, network);
        queue.start();
        return queue;
    }
}
