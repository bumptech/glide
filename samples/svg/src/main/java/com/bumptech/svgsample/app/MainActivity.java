package com.bumptech.svgsample.app;

import android.app.Activity;
import android.graphics.drawable.PictureDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.caverock.androidsvg.SVG;

import java.io.File;
import java.io.InputStream;

/**
 * Displays an SVG image loaded from an android raw resource.
 */
public class MainActivity extends Activity {
    private static final String TAG = "SVGActivity";

    ImageView imageViewRes;
    ImageView imageViewNet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageViewRes = (ImageView) findViewById(R.id.svg_image_view1);
        imageViewNet = (ImageView) findViewById(R.id.svg_image_view2);
    }

    @Override
    protected void onStart() {
        super.onStart();
        reload();
    }

    public void clearCache(View v) {
        Log.w(TAG, "clearing cache");
        Glide.clear(imageViewRes);
        Glide.clear(imageViewNet);
        Glide.get(this).clearMemory();
        File cacheDir = Glide.getPhotoCacheDir(this);
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
        ((TextView) findViewById(R.id.button)).setText(getString(R.string.scaleType, imageViewRes.getScaleType()));
        loadRes();
        loadNet();
    }

    private void loadRes() {
        Glide.with(this)
                .using(Glide.buildStreamModelLoader(Integer.class, this), InputStream.class)
                .load(R.raw.android_toy_h)
                .as(SVG.class)
                .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                        // SVG cannot be serialized so it's not worth to cache it
                        // and the getResources() should be fast enough when acquiring the InputStream
                .decoder(new SvgDecoder())
                .placeholder(R.drawable.image_loading)
                .error(R.drawable.image_error)
                .animate(android.R.anim.fade_in)
                .listener(new SvgSoftwareLayerSetter<Integer, PictureDrawable>())
                .into(imageViewRes);
    }

    private void loadNet() {
        Glide.with(this)
                .using(Glide.buildStreamModelLoader(String.class, this), InputStream.class)
                .load("http://www.clker.com/cliparts/u/Z/2/b/a/6/android-toy-h.svg")
                .as(SVG.class)
                .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        // SVG cannot be serialized so it's not worth to cache it
                .sourceEncoder(new StreamEncoder())
                        // however loading from the network can be cached via StreamEncoder
                .cacheDecoder(new FileToStreamDecoder<SVG>(new SvgDecoder()))
                .decoder(new SvgDecoder())
                .placeholder(R.drawable.image_loading)
                .error(R.drawable.image_error)
                .animate(android.R.anim.fade_in)
                .listener(new SvgSoftwareLayerSetter<String, PictureDrawable>())
                .into(imageViewNet);
    }
}
