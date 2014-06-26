package com.bumptech.glide.manager;

import android.annotation.TargetApi;
import android.app.Fragment;
import com.bumptech.glide.RequestManager;

@TargetApi(11)
public class RequestManagerFragment extends Fragment {
    private RequestManager requestManager;

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    public RequestManager getRequestManager() {
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
