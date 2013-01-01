/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.presenter;

import android.widget.ImageView;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 10:04 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ImageSetCallback {
    public void onImageSet(ImageView view, boolean fromCache);
}
