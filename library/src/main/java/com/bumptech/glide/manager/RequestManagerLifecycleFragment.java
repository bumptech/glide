package com.bumptech.glide.manager;

import com.bumptech.glide.RequestManager;

public interface RequestManagerLifecycleFragment {

    public void setRequestManager(RequestManager requestManager);

    public RequestManager getRequestManager();

    public void onStart();

    public void onStop();

    public void onDestroy();
}
