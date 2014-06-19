package com.bumptech.glide.manager;

import android.support.v4.app.Fragment;

class SupportRequestManagerFragment extends Fragment {
    private LifecycleRequestManager requestManager;

    public void setRequestManager(LifecycleRequestManager requestManager) {
        this.requestManager = requestManager;
    }

    public LifecycleRequestManager getRequestManager() {
        return requestManager;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (requestManager != null) {
            requestManager.onStart();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (requestManager != null) {
            requestManager.onStop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestManager != null) {
            requestManager.onDestroy();
        }
    }
}
