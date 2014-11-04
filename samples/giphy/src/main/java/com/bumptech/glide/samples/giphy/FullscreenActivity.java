package com.bumptech.glide.samples.giphy;

import com.google.gson.Gson;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.BitmapToGlideDrawableTranscoder;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

/**
 * An {@link android.app.Activity} for displaying full size original GIFs.
 */
public class FullscreenActivity extends Activity {
    private static final String EXTRA_RESULT_JSON = "result_json";
    private GifDrawable gifDrawable;

    public static Intent getIntent(Context context, Api.GifResult result) {
        Intent intent = new Intent(context, FullscreenActivity.class);
        intent.putExtra(EXTRA_RESULT_JSON, new Gson().toJson(result));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_activity);

        String resultJson = getIntent().getStringExtra(EXTRA_RESULT_JSON);
        final Api.GifResult result = new Gson().fromJson(resultJson, Api.GifResult.class);

        ImageView gifView = (ImageView) findViewById(R.id.fullscreen_gif);

        gifView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("giphy_url", result.images.original.url);
                clipboard.setPrimaryClip(clip);

                if (gifDrawable != null) {
                    if (gifDrawable.isRunning()) {
                        gifDrawable.stop();
                    } else {
                        gifDrawable.start();
                    }
                }
            }
        });

        Glide.with(this)
                .load(result.images.original.url)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .thumbnail(Glide.with(this)
                        .load(result)
                        .asBitmap()
                        .transcode(new BitmapToGlideDrawableTranscoder(this), GlideDrawable.class)
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                )
                .listener(new RequestListener<Object, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Object model, Target<GlideDrawable> target,
                            boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Object model, Target<GlideDrawable> target,
                            boolean isFromMemoryCache, boolean isFirstResource) {
                        if (resource instanceof GifDrawable) {
                            gifDrawable = (GifDrawable) resource;
                        } else {
                            gifDrawable = null;
                        }
                        return false;
                    }
                })
                .into(gifView);
    }
}
