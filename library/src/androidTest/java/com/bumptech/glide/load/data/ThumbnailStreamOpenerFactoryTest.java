package com.bumptech.glide.load.data;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.net.Uri;
import android.provider.MediaStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ThumbnailStreamOpenerFactoryTest {
    private MediaStoreThumbFetcher.ThumbnailStreamOpenerFactory factory;
    private Uri uri;

    @Before
    public void setUp() {
        factory = new MediaStoreThumbFetcher.ThumbnailStreamOpenerFactory();
        uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "123");
    }

    @Test
    public void testFactoryReturnsNullIfWidthTooLarge() {
        MediaStoreThumbFetcher.ThumbnailStreamOpener fetcher = factory.build(uri, 10000, 1);
        assertNull(fetcher);
    }

    @Test
    public void testFactoryReturnsNullIfHeightTooLarge() {
        MediaStoreThumbFetcher.ThumbnailStreamOpener fetcher = factory.build(uri, 1, 10000);
        assertNull(fetcher);
    }

    @Test
    public void testFactoryReturnsNullForNonMediaStoreUri() {
        MediaStoreThumbFetcher.ThumbnailStreamOpener fetcher = factory.build(Uri.EMPTY, 1, 1);
        assertNull(fetcher);
    }

    @Test
    public void testFactoryReturnsImageFetcherForImageUri() {
        MediaStoreThumbFetcher.ThumbnailStreamOpener fetcher = factory.build(uri, 1, 1);
        assertNotNull(fetcher);
    }

    @Test
    public void testFactoryReturnsVideoFetcherForVideoUri() {
        uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "123");
        MediaStoreThumbFetcher.ThumbnailStreamOpener fetcher = factory.build(uri, 1, 1);
        assertNotNull(fetcher);
    }
}