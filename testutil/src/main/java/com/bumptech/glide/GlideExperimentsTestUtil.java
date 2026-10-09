package com.bumptech.glide;

import com.bumptech.glide.GlideExperiments.Experiment;

/** Builds real {@link GlideExperiments} for tests outside this package. */
public final class GlideExperimentsTestUtil {
  private GlideExperimentsTestUtil() {}

  /** Returns experiments with only {@code experiment} enabled. */
  public static GlideExperiments withExperiment(Experiment experiment) {
    return new GlideExperiments.Builder().add(experiment).build();
  }

  /** Returns experiments with nothing enabled. */
  public static GlideExperiments empty() {
    return new GlideExperiments.Builder().build();
  }
}
