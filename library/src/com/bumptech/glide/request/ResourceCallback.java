package com.bumptech.glide.request;

import com.bumptech.glide.Resource;

public interface ResourceCallback<Z> {

    public void onResourceReady(Resource<Z> resource);

    public void onException(Exception e);
}
