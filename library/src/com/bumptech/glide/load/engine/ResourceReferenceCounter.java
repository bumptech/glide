package com.bumptech.glide.load.engine;

import com.bumptech.glide.Resource;

public interface ResourceReferenceCounter {

    public void acquireResource(Resource resource);

    public void releaseResource(Resource resource);
}
