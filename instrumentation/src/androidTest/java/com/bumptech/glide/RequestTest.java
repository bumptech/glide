package com.bumptech.glide;

import static com.bumptech.glide.test.Matchers.anyDrawable;
import static com.bumptech.glide.test.Matchers.anyDrawableTarget;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import com.bumptech.glide.test.WaitModelLoader;
import com.bumptech.glide.test.WaitModelLoader.WaitModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests the behaviors of Requests of all types. */
@RunWith(AndroidJUnit4.class)
public class RequestTest {
  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();
  @Mock private RequestListener<Drawable> requestListener;
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private Context context;
  private ImageView imageView;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();
    imageView = new ImageView(context);
    imageView.measure(100, 100);
    imageView.layout(0, 0, 100, 100);

    // Some emulators only have a single resize thread, so waiting on a latch will block them
    // forever.
    Glide.init(
        context, new GlideBuilder().setSourceExecutor(GlideExecutor.newUnlimitedSourceExecutor()));
  }

  @Test
  public void clear_withSingleRequest_nullsOutDrawableInView() {
    concurrency.loadOnMainThread(GlideApp.with(context).load(ResourceIds.raw.canonical), imageView);
    assertThat(imageView.getDrawable()).isNotNull();

    concurrency.clearOnMainThread(imageView);
    assertThat(imageView.getDrawable()).isNull();
  }

  @Test
  public void clear_withRequestWithThumbnail_nullsOutDrawableInView() {
    concurrency.loadOnMainThread(
        GlideApp.with(context)
            .load(ResourceIds.raw.canonical)
            .thumbnail(GlideApp.with(context).load(ResourceIds.raw.canonical).override(100, 100)),
        imageView);
    assertThat(imageView.getDrawable()).isNotNull();

    concurrency.clearOnMainThread(imageView);
    assertThat(imageView.getDrawable()).isNull();
  }

  @Test
  public void onStop_withSingleRequest_doesNotNullOutDrawableInView() {
    concurrency.loadOnMainThread(GlideApp.with(context).load(ResourceIds.raw.canonical), imageView);
    assertThat(imageView.getDrawable()).isNotNull();

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.with(context).onStop();
          }
        });
    assertThat(imageView.getDrawable()).isNotNull();
  }

  @Test
  public void onStop_withRequestWithThumbnail_doesNotNullOutDrawableInView() {
    concurrency.loadOnMainThread(
        GlideApp.with(context)
            .load(ResourceIds.raw.canonical)
            .thumbnail(GlideApp.with(context).load(ResourceIds.raw.canonical).override(100, 100)),
        imageView);
    assertThat(imageView.getDrawable()).isNotNull();

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.with(context).onStop();
          }
        });
    assertThat(imageView.getDrawable()).isNotNull();
  }

  @Test
  public void onStop_withSingleRequestInProgress_nullsOutDrawableInView() {
    final WaitModel<Integer> model = WaitModelLoader.Factory.waitOn(ResourceIds.raw.canonical);
    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.with(context).load(ResourceIds.raw.canonical).into(imageView);
          }
        });
    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.with(context).onStop();
          }
        });
    assertThat(imageView.getDrawable()).isNull();
    model.countDown();
  }

  @Test
  public void onStop_withRequestWithThumbnailBothInProgress_nullsOutDrawableInView() {
    final WaitModel<Integer> model = WaitModelLoader.Factory.waitOn(ResourceIds.raw.canonical);
    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.with(context)
                .load(model)
                .thumbnail(GlideApp.with(context).load(model).override(100, 100))
                .into(imageView);
          }
        });

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.with(context).onStop();
          }
        });
    assertThat(imageView.getDrawable()).isNull();
    model.countDown();
  }

  /** Tests #2555. */
  @Test
  public void clear_withRequestWithOnlyFullInProgress_nullsOutDrawableInView() {
    final WaitModel<Integer> mainModel = WaitModelLoader.Factory.waitOn(ResourceIds.raw.canonical);
    concurrency.loadUntilFirstFinish(
        GlideApp.with(context)
            .load(mainModel)
            .listener(requestListener)
            .thumbnail(
                GlideApp.with(context)
                    .load(ResourceIds.raw.canonical)
                    .listener(requestListener)
                    .override(100, 100)),
        imageView);

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.with(context).clear(imageView);
          }
        });

    verify(requestListener, never())
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.DATA_DISK_CACHE),
            anyBoolean());
    verify(requestListener, never())
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.RESOURCE_DISK_CACHE),
            anyBoolean());
    assertThat(imageView.getDrawable()).isNull();
    mainModel.countDown();
  }

  @Test
  public void clear_withRequestWithOnlyFullInProgress_doesNotNullOutDrawableInView() {
    final WaitModel<Integer> mainModel = WaitModelLoader.Factory.waitOn(ResourceIds.raw.canonical);
    concurrency.loadUntilFirstFinish(
        GlideApp.with(context)
            .load(mainModel)
            .listener(requestListener)
            .thumbnail(
                GlideApp.with(context)
                    .load(ResourceIds.raw.canonical)
                    .listener(requestListener)
                    .override(100, 100)),
        imageView);

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.with(context).onStop();
          }
        });

    verify(requestListener, never())
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.DATA_DISK_CACHE),
            anyBoolean());
    verify(requestListener, never())
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.RESOURCE_DISK_CACHE),
            anyBoolean());
    assertThat(imageView.getDrawable()).isNotNull();
    mainModel.countDown();
  }

  @Test
  public void onStop_withRequestWithOnlyThumbnailInProgress_doesNotNullOutDrawableInView() {
    final WaitModel<Integer> thumbModel = WaitModelLoader.Factory.waitOn(ResourceIds.raw.canonical);
    concurrency.loadUntilFirstFinish(
        GlideApp.with(context)
            .load(ResourceIds.raw.canonical)
            .listener(requestListener)
            .thumbnail(
                GlideApp.with(context)
                    .load(thumbModel)
                    .listener(requestListener)
                    .override(100, 100)),
        imageView);

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.with(context).onStop();
          }
        });

    verify(requestListener, never())
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.DATA_DISK_CACHE),
            anyBoolean());
    verify(requestListener, never())
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.RESOURCE_DISK_CACHE),
            anyBoolean());

    // Only requests that are running are paused in onStop. The full request should take priority
    // over the thumbnail request. Therefore, if the full request is finished in onStop, it should
    // not be cleared, even if the thumbnail request is still running.
    assertThat(imageView.getDrawable()).isNotNull();
    thumbModel.countDown();
  }
}
