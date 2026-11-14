package com.bumptech.glide.load.engine;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.core.util.Pools;
import com.bumptech.glide.GlideContext;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.util.GlideSuppliers.GlideSupplier;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 24)
public class DecodeJobPriorityOverrideTest {
  @Test
  @Config(sdk = 23)
  public void init_onApi23_doesNotReadApi24PriorityOption() {
    Options options = mock(Options.class);
    DecodeJob<Object> job =
        new DecodeJob<>(mock(DecodeJob.DiskCacheProvider.class), new Pools.SimplePool<>(1));

    job.init(
        mock(GlideContext.class),
        new Object(),
        null,
        null,
        1,
        1,
        Object.class,
        Object.class,
        Priority.NORMAL,
        DiskCacheStrategy.NONE,
        Collections.emptyMap(),
        false,
        false,
        false,
        options,
        null,
        0);

    verify(options, never()).get(DecodeJob.GLIDE_THREAD_PRIORITY_OVERRIDE);
  }

  @Test
  public void getThreadPriorityOverride_withoutOption_returnsNull() {
    assertThat(DecodeJob.Api24Impl.getThreadPriorityOverride(new Options())).isNull();
  }

  @Test
  public void getThreadPriorityOverride_queriesSupplierEachTime() {
    AtomicReference<Integer> priority = new AtomicReference<>(5);
    Options options = new Options().set(DecodeJob.GLIDE_THREAD_PRIORITY_OVERRIDE, priority::get);

    GlideSupplier<Integer> override = DecodeJob.Api24Impl.getThreadPriorityOverride(options);

    assertThat(override.get()).isEqualTo(5);
    priority.set(10);
    assertThat(override.get()).isEqualTo(10);
    priority.set(null);
    assertThat(override.get()).isNull();
  }
}
