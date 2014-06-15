package com.bumptech.glide.manager;

public interface RequestManagerLifecycleFragment {

    public void setRequestManager(LifecycleRequestManager requestManager);

    public LifecycleRequestManager getRequestManager();

    public void onStart();

    public void onStop();

    public void onDestroy();
}
