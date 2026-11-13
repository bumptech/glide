package com.bumptech.glide.load.engine;

interface EngineJobListener {

  void onEngineJobComplete(EngineJob<?> engineJob, EngineKey key, EngineResource<?> resource);

  void onEngineJobCancelled(EngineJob<?> engineJob, EngineKey key);
}
