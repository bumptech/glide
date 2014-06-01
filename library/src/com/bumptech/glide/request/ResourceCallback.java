package com.bumptech.glide.request;

import com.bumptech.glide.Resource;

public interface ResourceCallback {

    public void onResourceReady(Resource resource);

    public void onException(Exception e);
}
