package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableEncoder;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import com.bumptech.glide.testutil.WaitModelLoader;
import com.bumptech.glide.testutil.WaitModelLoader.WaitModel;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ErrorHandlingTest {

  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();
  @Mock private RequestListener<Drawable> requestListener;
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();

  private Context context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();
  }

  private WaitForErrorStrategy initializeGlideWithWaitForErrorStrategy() {
    WaitForErrorStrategy strategy = new WaitForErrorStrategy();
    Glide.init(
        context,
        new GlideBuilder()
            .setAnimationExecutor(
                GlideExecutor.newAnimationExecutor(/* threadCount= */ 1, strategy))
            .setSourceExecutor(GlideExecutor.newSourceExecutor(strategy))
            .setDiskCacheExecutor(GlideExecutor.newDiskCacheExecutor(strategy)));
    Glide.get(context)
        .getRegistry()
        .prepend(Bitmap.class, new FailEncoder())
        .prepend(
            BitmapDrawable.class,
            new BitmapDrawableEncoder(Glide.get(context).getBitmapPool(), new FailEncoder()));

    return strategy;
  }

  // ResourceEncoders are expected not to throw and to return true or false. If they do throw, it's
  // a developer error, so we expect UncaughtThrowableStrategy to be called.
  @Test
  public void load_whenEncoderFails_callsUncaughtThrowableStrategy() {
    WaitForErrorStrategy strategy = initializeGlideWithWaitForErrorStrategy();

    concurrency.get(
        Glide.with(context).load(ResourceIds.raw.canonical).listener(requestListener).submit());

    // Writing to the disk cache and therefore the exception caused by our FailEncoder may happen
    // after the request completes, so we should wait for the expected error explicitly.
    ConcurrencyHelper.waitOnLatch(strategy.latch);
    assertThat(strategy.error).isEqualTo(FailEncoder.TO_THROW);

    verify(requestListener, never())
        .onLoadFailed(
            any(GlideException.class),
            any(),
            ArgumentMatchers.<Target<Drawable>>any(),
            anyBoolean());
  }

  @Test
  public void load_whenLoadSucceeds_butEncoderFails_doesNotCallOnLoadFailed() {
    WaitForErrorStrategy strategy = initializeGlideWithWaitForErrorStrategy();

    concurrency.get(
        Glide.with(context).load(ResourceIds.raw.canonical).listener(requestListener).submit());

    verify(requestListener)
        .onResourceReady(
            ArgumentMatchers.<Drawable>any(),
            any(),
            ArgumentMatchers.<Target<Drawable>>any(),
            any(DataSource.class),
            anyBoolean());
    verify(requestListener, never())
        .onLoadFailed(
            any(GlideException.class),
            any(),
            ArgumentMatchers.<Target<Drawable>>any(),
            anyBoolean());
  }

  @Test
  public void clearRequest_withError_afterPrimaryFails_clearsErrorRequest() {
    WaitModel<Integer> errorModel = WaitModelLoader.waitOn(ResourceIds.raw.canonical);

    FutureTarget<Drawable> target =
        Glide.with(context)
            .load((Object) null)
            .error(Glide.with(context).load(errorModel).listener(requestListener))
            .submit();

    Glide.with(context).clear(target);
    errorModel.countDown();

    // Make sure any pending requests run.
    concurrency.pokeMainThread();
    Glide.tearDown();
    // Make sure that any callbacks posted back to the main thread run.
    concurrency.pokeMainThread();
  }

  private static final class WaitForErrorStrategy implements UncaughtThrowableStrategy {
    final CountDownLatch latch = new CountDownLatch(1);
    @Nullable Throwable error = null;

    @Override
    public void handle(Throwable t) {
      if (error != null) {
        throw new IllegalArgumentException("Received second error", t);
      }
      error = t;
      latch.countDown();
    }
  }

  private static final class FailEncoder implements ResourceEncoder<Bitmap> {

    static final RuntimeException TO_THROW = new RuntimeException();

    @NonNull
    @Override
    public EncodeStrategy getEncodeStrategy(@NonNull Options options) {
      return EncodeStrategy.TRANSFORMED;
    }

    @Override
    public boolean encode(
        @NonNull Resource<Bitmap> data, @NonNull File file, @NonNull Options options) {
      throw TO_THROW;
    }
  }
}
