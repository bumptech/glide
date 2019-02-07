package com.bumptech.glide.load.resource.gif;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest.permission;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import com.bumptech.glide.load.resource.gif.GifDrawable.GifState;
import com.bumptech.glide.load.resource.gif.GifFrameLoader.OnEveryFrameListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import com.bumptech.glide.util.Preconditions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GifDrawableTest {
  @Rule public final TestName testName = new TestName();
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  @Rule public final GrantPermissionRule grantPermissionRule;
  private final ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper();

  {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
      grantPermissionRule = GrantPermissionRule.grant(permission.SYSTEM_ALERT_WINDOW);
    } else {
      grantPermissionRule = GrantPermissionRule.grant();
    }
  }

  private Context context;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
  }

  @Test
  public void loadGif_withInterlacedTransparentGif_sizeOriginal_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        concurrencyHelper.get(
            GlideApp.with(context)
                .asGif()
                .load(ResourceIds.raw.interlaced_transparent_gif)
                .submit());
    assertThat(gifDrawable).isNotNull();
  }

  @Test
  public void loadGif_withInterlacedTransparentGif_downsampled_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        concurrencyHelper.get(
            GlideApp.with(context)
                .asGif()
                .load(ResourceIds.raw.interlaced_transparent_gif)
                .submit(10, 10));
    assertThat(gifDrawable).isNotNull();
  }

  @Test
  public void loadGif_withTransparentGif_sizeOriginal_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        concurrencyHelper.get(
            GlideApp.with(context).asGif().load(ResourceIds.raw.transparent_gif).submit());
    assertThat(gifDrawable).isNotNull();
  }

  @Test
  public void loadGif_withTransparentGif_downsampled_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        concurrencyHelper.get(
            GlideApp.with(context).asGif().load(ResourceIds.raw.transparent_gif).submit(10, 10));
    assertThat(gifDrawable).isNotNull();
  }

  @Test
  public void loadGif_withOpaqueGif_sizeOriginal_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        concurrencyHelper.get(
            GlideApp.with(context).asGif().load(ResourceIds.raw.opaque_gif).submit());
    assertThat(gifDrawable).isNotNull();
  }

  @Test
  public void loadGif_withOpaqueGif_downsampled_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        concurrencyHelper.get(
            GlideApp.with(context).asGif().load(ResourceIds.raw.opaque_gif).submit(10, 10));
    assertThat(gifDrawable).isNotNull();
  }

  @Test
  public void loadGif_withOpaqueInterlacedGif_sizeOriginal_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        concurrencyHelper.get(
            GlideApp.with(context).asGif().load(ResourceIds.raw.opaque_interlaced_gif).submit());
    assertThat(gifDrawable).isNotNull();
  }

  @Test
  public void loadGif_withOpaqueInterlacedGif_downsampled_succeeds()
      throws ExecutionException, InterruptedException {
    GifDrawable gifDrawable =
        concurrencyHelper.get(
            GlideApp.with(context)
                .asGif()
                .load(ResourceIds.raw.opaque_interlaced_gif)
                .submit(10, 10));
    assertThat(gifDrawable).isNotNull();
  }

  @Test
  public void loadGif_intoImageView_afterStop_restartsGif()
      throws ExecutionException, InterruptedException {
    // Mimic the state the Drawable can get into if it was loaded into a View previously and stopped
    // so that it ended up with a pending frame that finished after the stop call.
    final GifDrawable gifDrawable =
        concurrencyHelper.get(
            GlideApp.with(context)
                .asGif()
                .load(ResourceIds.raw.dl_world_anim)
                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL));

    final CountDownLatch waitForGifFrame = new CountDownLatch(1);
    // Starting/Stopping loads in GIFs must happen on the main thread.
    concurrencyHelper.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            // Make sure a frame is loaded while the drawable is stopped.
            GifState gifState =
                (GifState) Preconditions.checkNotNull(gifDrawable.getConstantState());
            gifState.frameLoader.setOnEveryFrameReadyListener(
                new OnEveryFrameListener() {
                  @Override
                  public void onFrameReady() {
                    waitForGifFrame.countDown();
                  }
                });
            gifDrawable.start();
            gifDrawable.stop();
          }
        });
    ConcurrencyHelper.waitOnLatch(waitForGifFrame);

    // Load the Drawable with the pending frame into a new View and make sure it ends up in the
    // running state.
    final ImageView imageView = new ImageView(context);
    concurrencyHelper.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            addViewToWindow(imageView);
          }
        });

    concurrencyHelper.loadOnMainThread(
        GlideApp.with(context).load(gifDrawable).override(Target.SIZE_ORIGINAL), imageView);

    GifDrawable drawableFromView = (GifDrawable) imageView.getDrawable();
    assertThat(drawableFromView.isRunning()).isTrue();

    drawableFromView.stop();
    gifDrawable.stop();
  }

  @SuppressWarnings("deprecation")
  private void addViewToWindow(View view) {
    WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
    layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
    layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
    layoutParams.type =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? LayoutParams.TYPE_APPLICATION_OVERLAY
            : Build.VERSION.SDK_INT == Build.VERSION_CODES.M
                ? LayoutParams.TYPE_TOAST
                : LayoutParams.TYPE_SYSTEM_ALERT;
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Preconditions.checkNotNull(windowManager).addView(view, layoutParams);
  }
}
