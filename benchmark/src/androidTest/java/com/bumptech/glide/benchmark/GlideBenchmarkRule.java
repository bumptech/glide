package com.bumptech.glide.benchmark;

import android.content.Context;
import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.testutil.TearDownGlide;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

final class GlideBenchmarkRule implements TestRule {
  private final TearDownGlide tearDownGlide = new TearDownGlide();
  private final BenchmarkRule benchmarkRule = new BenchmarkRule();

  private final TestRule ruleChain = RuleChain.outerRule(benchmarkRule).around(tearDownGlide);

  @NotNull
  @Override
  public Statement apply(@NotNull Statement base, @NotNull Description description) {
    return ruleChain.apply(base, description);
  }

  void pauseTiming() {
    benchmarkRule.getState().pauseTiming();
  }

  void resumeTiming() {
    benchmarkRule.getState().resumeTiming();
  }

  BenchmarkRule getBenchmark() {
    return benchmarkRule;
  }

  interface LoadStep<BeforeDataT> {
    Object getModel(BeforeDataT beforeData) throws Exception;
  }

  interface BeforeStep<BeforeDataT> {
    BeforeDataT act() throws Exception;
  }

  interface AfterStep<BeforeDataT> {
    void act(BeforeDataT beforeData) throws Exception;
  }

  <T> void runBenchmark(BeforeStep<T> beforeStep, AfterStep<T> afterStep) throws Exception {
    runBenchmark(
        beforeStep,
        new LoadStep<T>() {
          @Override
          public Object getModel(T beforeData) {
            return beforeData;
          }
        },
        afterStep);
  }

  <T> void runBenchmark(BeforeStep<T> beforeStep, LoadStep<T> loadStep, AfterStep<T> afterStep)
      throws Exception {
    BenchmarkState state = benchmarkRule.getState();
    Context app = ApplicationProvider.getApplicationContext();
    while (state.keepRunning()) {
      state.pauseTiming();
      Glide.get(app);
      T beforeData = beforeStep.act();
      state.resumeTiming();

      Glide.with(app)
          .load(loadStep.getModel(beforeData))
          .diskCacheStrategy(DiskCacheStrategy.NONE)
          .override(Target.SIZE_ORIGINAL)
          .submit()
          .get(15, TimeUnit.SECONDS);

      state.pauseTiming();
      tearDownGlide.tearDownGlide();
      afterStep.act(beforeData);
      state.resumeTiming();
    }
  }
}
