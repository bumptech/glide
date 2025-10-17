package com.bumptech.glide.testutil;

import com.bumptech.glide.testutil.WaitModelLoader.WaitModel;
import java.util.ArrayList;
import java.util.List;
import org.junit.rules.ExternalResource;

/** Makes sure that all {@link WaitModel}s created by it are unblocked before the test ends. */
public final class WaitModelLoaderRule extends ExternalResource {
  private final List<WaitModel<?>> waitModels = new ArrayList<>();

  public <T> WaitModel<T> waitOn(T model) {
    WaitModel<T> waitModel = WaitModelLoader.waitOn(model);
    waitModels.add(waitModel);
    return waitModel;
  }

  @Override
  protected void after() {
    super.after();
    for (WaitModel<?> waitModel : waitModels) {
      waitModel.countDown();
    }
  }
}
