package com.bumptech.glide.manager;

import android.support.v4.app.Fragment;
import com.bumptech.glide.RequestManager;

/**
 * A view-less {@link android.support.v4.app.Fragment} used to safely store an
 * {@link com.bumptech.glide.RequestManager} that can be used to start, stop and manage Glide requests started for
 * targets within the fragment or activity this fragment is a child of.
 *
 * @see com.bumptech.glide.manager.RequestManagerFragment
 * @see com.bumptech.glide.manager.RequestManagerRetriever
 * @see com.bumptech.glide.RequestManager
 */
public class SupportRequestManagerFragment extends Fragment {
    private RequestManager requestManager;

    /**
     * Sets the current {@link com.bumptech.glide.RequestManager}.
     *
     * @param requestManager The manager to set.
     */
    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    /**
     * Returns the current {@link com.bumptech.glide.RequestManager} or null if none is set.
     */
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
