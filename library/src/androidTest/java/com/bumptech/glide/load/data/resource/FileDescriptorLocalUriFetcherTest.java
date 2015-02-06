package com.bumptech.glide.load.data.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.FileDescriptorLocalUriFetcher;
import com.bumptech.glide.tests.ContentResolverShadow;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = { ContentResolverShadow.class })
public class FileDescriptorLocalUriFetcherTest {

    @Test
    public void testLoadsFileDescriptor() throws Exception {
        final Context context = RuntimeEnvironment.application;
        final Uri uri = Uri.parse("file://nothing");

        ContentResolver contentResolver = context.getContentResolver();
        ContentResolverShadow shadow = (ContentResolverShadow) ShadowExtractor.extract(contentResolver);

        AssetFileDescriptor assetFileDescriptor = mock(AssetFileDescriptor.class);
        ParcelFileDescriptor parcelFileDescriptor = mock(ParcelFileDescriptor.class);
        when(assetFileDescriptor.getParcelFileDescriptor()).thenReturn(parcelFileDescriptor);
        shadow.registerFileDescriptor(uri, assetFileDescriptor);

        FileDescriptorLocalUriFetcher fetcher = new FileDescriptorLocalUriFetcher(context, uri);
        ParcelFileDescriptor descriptor = fetcher.loadData(Priority.NORMAL);

        assertEquals(parcelFileDescriptor, descriptor);
    }

}
