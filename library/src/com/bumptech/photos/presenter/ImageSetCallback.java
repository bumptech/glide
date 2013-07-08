/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.presenter;

import android.widget.ImageView;

/**
 * A callback interface used to perform some action when an {@link ImagePresenter} sets an
 * {@link android.widget.ImageView} object's bitmap
 */
public interface ImageSetCallback {

    /**
     * The method called when a bitmap is set
     *
     * @param view The view that will display the bitmap
     * @param fromCache True iff the bitmap load completed without a placeholder being shown.
     */
    public void onImageSet(ImageView view, boolean fromCache);
}
