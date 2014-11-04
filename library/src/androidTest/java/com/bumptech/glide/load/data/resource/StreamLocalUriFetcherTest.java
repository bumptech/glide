package com.bumptech.glide.load.data.resource;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.StreamLocalUriFetcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class StreamLocalUriFetcherTest {

    @Test
    public void testLoadsInputStream() throws Exception {
        final Context context = Robolectric.application;
        Uri uri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
        StreamLocalUriFetcher fetcher = new StreamLocalUriFetcher(context, uri);
        InputStream is = fetcher.loadData(Priority.NORMAL);
        assertNotNull(is);
        assertNotNull(BitmapFactory.decodeStream(is));
        is.close();
    }
}
