package com.bumptech.swatchsample.app;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.integration.palette.PaletteBitmap;
import com.bumptech.glide.integration.palette.PaletteBitmapTranscoder;

/**
 * Displays images loaded from the Internet and adjust colors to them.
 *
 * @see com.bumptech.swatchsample.app.MainActivity#onCreate(android.os.Bundle)
 * @see com.bumptech.swatchsample.app.PaletteAdapter.ViewHolder
 */
public class MainActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BitmapRequestBuilder<Uri, PaletteBitmap> glideBuilder = Glide
                .with(this)
                .from(Uri.class)
                .asBitmap()
                .transcode(new PaletteBitmapTranscoder(this), PaletteBitmap.class);
        Uri[] urls = new LoremPixelUrlGenerator().generateAll();
        setListAdapter(new PaletteAdapter(urls, glideBuilder));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = (Uri) l.getAdapter().getItem(position);
        Intent intent = new Intent(this, DetailActivity.class);
        intent.setData(uri);
        startActivity(intent);
    }
}
