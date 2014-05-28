package com.bumptech.glide.resize;

public interface EngineJobListener {

    public void onEngineJobComplete(Key key);

    public void onEngineJobCancelled(Key key);
}
