package com.bumptech.glide.resize;

interface ResourceCallback {

    public void onResourceReady(Resource resource);

    public void onException(Exception e);
}
