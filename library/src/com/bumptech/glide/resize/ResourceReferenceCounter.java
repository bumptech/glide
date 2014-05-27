package com.bumptech.glide.resize;

public interface ResourceReferenceCounter {

    public void acquireResource(Resource resource);

    public void releaseResource(Resource resource);
}
