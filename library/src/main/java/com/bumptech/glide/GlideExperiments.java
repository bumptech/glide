package com.bumptech.glide;

import androidx.annotation.Nullable;
import com.bumptech.glide.util.Synthetic;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of a set of Experimental features that may be enabled in Glide, simplifying the
 * process of adding and removing them.
 *
 * <p>This is an experimental API, it may be removed at any point without deprecation or other
 * notice.
 */
// non-final for mocking
public class GlideExperiments {

  private final Map<Class<?>, Experiment> experiments;

  @Synthetic
  GlideExperiments(Builder builder) {
    this.experiments =
        Collections.unmodifiableMap(new HashMap<Class<?>, Experiment>(builder.experiments));
  }

  @SuppressWarnings("unchecked")
  @Nullable
  <T extends Experiment> T get(Class<T> clazz) {
    return (T) experiments.get(clazz);
  }

  /**
   * Returns {@code true} if the given experiment is enabled.
   *
   * <p>This is an experimental API, it may be removed at any point without deprecation or other
   * notice.
   */
  public boolean isEnabled(Class<? extends Experiment> clazz) {
    return experiments.containsKey(clazz);
  }

  interface Experiment {}

  static final class Builder {
    private final Map<Class<?>, Experiment> experiments = new HashMap<>();

    Builder update(Experiment experiment, boolean isEnabled) {
      if (isEnabled) {
        add(experiment);
      } else {
        experiments.remove(experiment.getClass());
      }
      return this;
    }

    Builder add(Experiment experiment) {
      experiments.put(experiment.getClass(), experiment);
      return this;
    }

    GlideExperiments build() {
      return new GlideExperiments(this);
    }
  }
}
