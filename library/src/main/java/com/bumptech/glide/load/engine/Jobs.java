package com.bumptech.glide.load.engine;

import androidx.annotation.VisibleForTesting;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class Jobs {
  private final Map<EngineKey, EngineJob<?>> jobs = new HashMap<>();
  private final Map<EngineKey, EngineJob<?>> onlyCacheJobs = new HashMap<>();

  @VisibleForTesting
  Map<EngineKey, EngineJob<?>> getAll() {
    return Collections.unmodifiableMap(jobs);
  }

  EngineJob<?> get(EngineKey key, boolean onlyRetrieveFromCache) {
    return getJobMap(onlyRetrieveFromCache).get(key);
  }

  void put(EngineKey key, EngineJob<?> job) {
    getJobMap(job.onlyRetrieveFromCache()).put(key, job);
  }

  void removeIfCurrent(EngineKey key, EngineJob<?> expected) {
    Map<EngineKey, EngineJob<?>> jobMap = getJobMap(expected.onlyRetrieveFromCache());
    if (expected.equals(jobMap.get(key))) {
      jobMap.remove(key);
    }
  }

  private Map<EngineKey, EngineJob<?>> getJobMap(boolean onlyRetrieveFromCache) {
    return onlyRetrieveFromCache ? onlyCacheJobs : jobs;
  }
}
