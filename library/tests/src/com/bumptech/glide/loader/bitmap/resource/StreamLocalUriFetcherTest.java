package com.bumptech.glide.loader.bitmap.resource;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.test.ActivityTestCase;

import java.io.InputStream;

public class StreamLocalUriFetcherTest extends ActivityTestCase {

    public void testLoadsInputStream() throws Exception {
        final Context context = getInstrumentation().getContext();
        Uri uri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
        StreamLocalUriFetcher fetcher = new StreamLocalUriFetcher(context, uri);
        InputStream is = fetcher.loadResource();
        assertNotNull(is);
        assertNotNull(BitmapFactory.decodeStream(is));
        is.close();
    }
}
