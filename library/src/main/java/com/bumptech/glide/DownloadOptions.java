package com.bumptech.glide;

import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;

import java.io.File;

interface DownloadOptions {
    public <Y extends Target<File>> Y downloadOnly(Y target);

    public FutureTarget<File> downloadOnly(int width, int height);
}
