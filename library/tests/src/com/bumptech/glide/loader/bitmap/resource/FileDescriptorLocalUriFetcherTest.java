package com.bumptech.glide.loader.bitmap.resource;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.test.ActivityTestCase;
import com.bumptech.glide.resize.Metadata;

import static org.mockito.Mockito.mock;

public class FileDescriptorLocalUriFetcherTest extends ActivityTestCase {

    public void testLoadsFileDescriptor() throws Exception {
        final Context context = getInstrumentation().getContext();
        Uri uri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
        FileDescriptorLocalUriFetcher fetcher = new FileDescriptorLocalUriFetcher(context, uri);
        ParcelFileDescriptor descriptor = fetcher.loadResource(mock(Metadata.class));
        assertNotNull(descriptor);
        descriptor.close();
    }

}
