package com.bumptech.glide.load.resource.gif;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable.GifState;
import com.bumptech.glide.load.resource.gif.GifFrameLoader.OnEveryFrameListener;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import com.bumptech.glide.util.Preconditions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GifDrawableTest {
  @Rule public TestName testName = new TestName();
  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();
  private Context context;
  private Handler mainHandler;

  @Before
  public void setUp() {
    context = getTargetContext();
    mainHandler = new Handler(Looper.getMainLooper());
  }

  @Test
  public void loadGif_withInterlacedTransparentGif_sizeOriginal_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        GlideApp.with(context)
            .asGif()
            .load(ResourceIds.raw.interlaced_transparent_gif)
            .submit()
            .get();
    assertThat(gifDrawable).isNotNull();
    gifDrawable.stop();
  }

  @Test
  public void loadGif_withInterlacedTransparentGif_downsampled_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        GlideApp.with(context)
            .asGif()
            .load(ResourceIds.raw.interlaced_transparent_gif)
            .submit(10, 10)
            .get();
    assertThat(gifDrawable).isNotNull();
    gifDrawable.stop();
  }

  @Test
  public void loadGif_withTransparentGif_sizeOriginal_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        GlideApp.with(context)
            .asGif()
            .load(ResourceIds.raw.transparent_gif)
            .submit()
            .get();
    assertThat(gifDrawable).isNotNull();
    gifDrawable.stop();
  }

  @Test
  public void loadGif_withTransparentGif_downsampled_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        GlideApp.with(context)
            .asGif()
            .load(ResourceIds.raw.transparent_gif)
            .submit(10, 10)
            .get();
    assertThat(gifDrawable).isNotNull();
    gifDrawable.stop();
  }

  @Test
  public void loadGif_withOpaqueGif_sizeOriginal_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        GlideApp.with(context)
            .asGif()
            .load(ResourceIds.raw.opaque_gif)
            .submit()
            .get();
    assertThat(gifDrawable).isNotNull();
    gifDrawable.stop();
  }

  @Test
  public void loadGif_withOpaqueGif_downsampled_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        GlideApp.with(context)
            .asGif()
            .load(ResourceIds.raw.opaque_gif)
            .submit(10, 10)
            .get();
    assertThat(gifDrawable).isNotNull();
    gifDrawable.stop();
  }

  @Test
  public void loadGif_withOpaqueInterlacedGif_sizeOriginal_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        GlideApp.with(context)
            .asGif()
            .load(ResourceIds.raw.opaque_interlaced_gif)
            .submit()
            .get();
    assertThat(gifDrawable).isNotNull();
    gifDrawable.stop();
  }

  @Test
  public void loadGif_withOpaqueInterlacedGif_downsampled_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        GlideApp.with(context)
            .asGif()
            .load(ResourceIds.raw.opaque_interlaced_gif)
            .submit(10, 10)
            .get();
    assertThat(gifDrawable).isNotNull();
    gifDrawable.stop();
  }

  @Test
  public void loadGif_intoImageView_afterStop_restartsGif()
      throws ExecutionException, InterruptedException {
    // Required for us to add a View to a Window.
    assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.M);

    // Mimic the state the Drawable can get into if it was loaded into a View previously and stopped
    // so that it ended up with a pending frame that finished after the stop call.
    final GifDrawable gifDrawable = GlideApp.with(context)
        .asGif()
        .load(ResourceIds.raw.dl_world_anim)
        .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        .get();
    final CountDownLatch waitForGifFrame = new CountDownLatch(1);
    // Starting/Stopping loads in GIFs must happen on the main thread.
    mainHandler.post(
        new Runnable() {
          @Override
          public void run() {
            // Make sure a frame is loaded while the drawable is stopped.
            GifState gifState =
                (GifState) Preconditions.checkNotNull(gifDrawable.getConstantState());
            gifState.frameLoader.setOnEveryFrameReadyListener(new OnEveryFrameListener() {
              @Override
              public void onFrameReady() {
                waitForGifFrame.countDown();
              }
            });
            gifDrawable.start();
            gifDrawable.stop();
          }
        });
    waitOrThrow(waitForGifFrame);

    // Load the Drawable with the pending frame into a new View and make sure it ends up in the
    // running state.
    final ImageView imageView = new ImageView(context);
    final WaitForLoad<Drawable> waitForLoad = new WaitForLoad<>();
    // Starting loads into Views must happen on the main thread.
    mainHandler
        .post(new Runnable() {
          @Override
          public void run() {
            addViewToWindow(imageView);
            GlideApp.with(context)
                .load(gifDrawable)
                .listener(waitForLoad)
                .override(Target.SIZE_ORIGINAL)
                .into(imageView);
          }
        });
    waitForLoad.await();

    GifDrawable drawableFromView = (GifDrawable) imageView.getDrawable();
    assertThat(drawableFromView.isRunning()).isTrue();

    gifDrawable.stop();
    drawableFromView.stop();
  }

  // LayoutParams.TYPE_SYSTEM_ALERT.
  @SuppressWarnings("deprecation")
  private void addViewToWindow(View view) {
    final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
    layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
    layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
    layoutParams.type = LayoutParams.TYPE_SYSTEM_ALERT;
    final WindowManager windowManager =
        (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Preconditions.checkNotNull(windowManager).addView(view, layoutParams);
  }

  private static void waitOrThrow(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        fail("Failed to reach expected condition in the alloted time.");
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private final class WaitForLoad<T> implements RequestListener<T> {
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    void await() {
      waitOrThrow(countDownLatch);
    }

    @Override
    public boolean onLoadFailed(@Nullable GlideException e, Object model,
        Target<T> target,
        boolean isFirstResource) {
      throw new RuntimeException(e);
    }

    @Override
    public boolean onResourceReady(T resource, Object model, Target<T> target,
        DataSource dataSource,
        boolean isFirstResource) {
      mainHandler.post(new Runnable() {
        @Override
        public void run() {
          countDownLatch.countDown();
        }
      });
      return false;
    }
  }
}
