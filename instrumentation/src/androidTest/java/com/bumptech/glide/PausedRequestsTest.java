package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.GlideRequests;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests how {@link com.bumptech.glide.request.Request}s behave when the corresponding {@link
 * RequestManager} is paused.
 */
public final class PausedRequestsTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = ApplicationProvider.getApplicationContext();

  @SuppressWarnings("unchecked")
  @Test
  public void load_withPlaceHolderSet_requestsPaused_displaysPlaceholder() {
    final ImageView imageView = new ImageView(context);

    final GlideRequests requests = GlideApp.with(context);
    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            requests.pauseAllRequests();
          }
        });

    final ColorDrawable expected = new ColorDrawable(Color.RED);
    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            requests.load(ResourceIds.drawable.bitmap_alias).placeholder(expected).into(imageView);
          }
        });

    assertThat(imageView.getDrawable()).isEqualTo(expected);
  }
}
