package com.bumptech.glide.resize;

public interface ResourceCallback<Z> {

    public void onResourceReady(Resource<Z> resource);

    public void onException(Exception e);
}
