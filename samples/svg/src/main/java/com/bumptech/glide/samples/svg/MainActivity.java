package com.bumptech.glide.samples.svg;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.util.Preconditions;
import java.io.File;

/** Displays an SVG image loaded from an android raw resource. */
public class MainActivity extends Activity {
  private static final String TAG = "SVGActivity";

  private ImageView imageViewRes;
  private ImageView imageViewNet;
  private RequestBuilder<PictureDrawable> requestBuilder;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    imageViewRes = (ImageView) findViewById(R.id.svg_image_view1);
    imageViewNet = (ImageView) findViewById(R.id.svg_image_view2);

    requestBuilder =
        GlideApp.with(this)
            .as(PictureDrawable.class)
            .placeholder(R.drawable.image_loading)
            .error(R.drawable.image_error)
            .transition(withCrossFade())
            .listener(new SvgSoftwareLayerSetter());
  }

  @Override
  protected void onStart() {
    super.onStart();
    reload();
  }

  public void clearCache(View v) {
    Log.w(TAG, "clearing cache");
    GlideRequests glideRequests = GlideApp.with(this);
    glideRequests.clear(imageViewRes);
    glideRequests.clear(imageViewNet);
    GlideApp.get(this).clearMemory();
    File cacheDir = Preconditions.checkNotNull(Glide.getPhotoCacheDir(this));
    if (cacheDir.isDirectory()) {
      for (File child : cacheDir.listFiles()) {
        if (!child.delete()) {
          Log.w(TAG, "cannot delete: " + child);
        }
      }
    }
    reload();
  }

  public void cycleScaleType(View v) {
    ImageView.ScaleType curr = imageViewRes.getScaleType();
    Log.w(TAG, "cycle: current=" + curr);
    ImageView.ScaleType[] all = ImageView.ScaleType.values();
    int nextOrdinal = (curr.ordinal() + 1) % all.length;
    ImageView.ScaleType next = all[nextOrdinal];
    Log.w(TAG, "cycle: next=" + next);
    imageViewRes.setScaleType(next);
    imageViewNet.setScaleType(next);
    reload();
  }

  private void reload() {
    Log.w(TAG, "reloading");
    ((TextView) findViewById(R.id.button))
        .setText(getString(R.string.scaleType, imageViewRes.getScaleType()));
    loadRes();
    loadNet();
  }

  private void loadRes() {
    Uri uri =
        Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://"
                + getPackageName()
                + "/"
                + R.raw.android_toy_h);
    requestBuilder.load(uri).into(imageViewRes);
  }

  private void loadNet() {
    Uri uri = Uri.parse("http://www.clker.com/cliparts/u/Z/2/b/a/6/android-toy-h.svg");
    requestBuilder.load(uri).into(imageViewNet);
  }
}
