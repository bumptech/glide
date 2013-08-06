/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.presenter;

import com.bumptech.glide.presenter.target.Target;

/**
 * A callback interface used to perform some action when an {@link ImagePresenter} sets a new bitmap in an
 * {@link android.widget.ImageView}
 */
public interface ImageReadyCallback {

    /**
     * The method called when a bitmap is set
     *
     * @param target The target that will display the bitmap
     * @param fromCache True iff the load completed without a placeholder being shown.
     */
    public void onImageReady(Target target, boolean fromCache);
}
