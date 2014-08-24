package com.bumptech.svgsample.app;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;

import java.io.InputStream;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = (ImageView) findViewById(R.id.svg_image_view);

        Glide.with(this)
                .using(Glide.buildStreamModelLoader(Integer.class, this), InputStream.class)
                .load(R.drawable.ic_launcher) // Some resourceId.
                .as(Svg.class)
                .transcode(new SvgDrawableTranscoder(), SvgDrawable.class)
                .decoder(new SvgDecoder())
                .encoder(new SvgEncoder())
                .sourceEncoder(new StreamEncoder())
                .cacheDecoder(new FileToStreamDecoder<Svg>(new SvgDecoder()))
                .transform(new SvgTransformation())
                .into(imageView);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
