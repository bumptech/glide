/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize;

import android.graphics.Bitmap;

/**
 * Created by IntelliJ IDEA.
 * User: sam
 * Date: 2/9/12
 * Time: 7:04 PM
 * To change this template use File | Settings | File Templates.
 */
public interface LoadedCallback {
    public void onLoadCompleted(Bitmap loaded);
    public void onLoadFailed(Exception e);
}
