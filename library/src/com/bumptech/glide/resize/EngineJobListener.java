package com.bumptech.glide.resize;

public interface EngineJobListener {

    public void onEngineJobComplete(String id);

    public void onEngineJobCancelled(String id);
}
