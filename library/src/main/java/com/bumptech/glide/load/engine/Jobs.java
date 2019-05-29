package com.bumptech.glide.load.engine;

import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.load.Key;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class Jobs {
  private final Map<Key, EngineJob<?>> jobs = new HashMap<>();
  private final Map<Key, EngineJob<?>> onlyCacheJobs = new HashMap<>();

  @VisibleForTesting
  Map<Key, EngineJob<?>> getAll() {
    return Collections.unmodifiableMap(jobs);
  }

  EngineJob<?> get(Key key, boolean onlyRetrieveFromCache) {
    return getJobMap(onlyRetrieveFromCache).get(key);
  }

  void put(Key key, EngineJob<?> job) {
    getJobMap(job.onlyRetrieveFromCache()).put(key, job);
  }

  void removeIfCurrent(Key key, EngineJob<?> expected) {
    Map<Key, EngineJob<?>> jobMap = getJobMap(expected.onlyRetrieveFromCache());
    if (expected.equals(jobMap.get(key))) {
      jobMap.remove(key);
    }
  }

  private Map<Key, EngineJob<?>> getJobMap(boolean onlyRetrieveFromCache) {
    return onlyRetrieveFromCache ? onlyCacheJobs : jobs;
  }
}
