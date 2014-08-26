package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;

interface EngineJobListener {

    public void onEngineJobComplete(Key key, EngineResource<?> resource);

    public void onEngineJobCancelled(EngineJob engineJob, Key key);
}
