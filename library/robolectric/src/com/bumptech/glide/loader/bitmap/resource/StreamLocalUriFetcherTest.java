package com.bumptech.glide.loader.bitmap.resource;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.bumptech.glide.resize.Metadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class StreamLocalUriFetcherTest {

    @Test
    public void testLoadsInputStream() throws Exception {
        final Context context = Robolectric.application;
        Uri uri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
        StreamLocalUriFetcher fetcher = new StreamLocalUriFetcher(context, uri);
        InputStream is = fetcher.loadResource(mock(Metadata.class));
        assertNotNull(is);
        assertNotNull(BitmapFactory.decodeStream(is));
        is.close();
    }
}
