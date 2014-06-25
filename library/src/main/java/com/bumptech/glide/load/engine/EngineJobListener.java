package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;

public interface EngineJobListener {

    public void onEngineJobComplete(Key key, Resource resource);

    public void onEngineJobCancelled(Key key);
}
