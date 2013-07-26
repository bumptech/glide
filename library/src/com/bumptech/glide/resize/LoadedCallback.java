/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize;

import android.graphics.Bitmap;

/**
 * An interface to handle loads completing successfully or failing
 */
public interface LoadedCallback {
    public void onLoadCompleted(Bitmap loaded);
    public void onLoadFailed(Exception e);
}
