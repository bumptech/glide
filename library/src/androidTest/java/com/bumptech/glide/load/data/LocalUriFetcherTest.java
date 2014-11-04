package com.bumptech.glide.load.data;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.bumptech.glide.Priority;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class LocalUriFetcherTest {
    private TestLocalUriFetcher fetcher;

    @Before
    public void setUp() {
        fetcher = new TestLocalUriFetcher(Robolectric.application, Uri.parse("content://empty"));
    }

    @Test
    public void testClosesDataOnCleanup() throws Exception {
        Closeable closeable = fetcher.loadData(Priority.NORMAL);
        fetcher.cleanup();

        verify(closeable).close();
    }

    @Test
    public void testDoesNotCloseNullData() throws IOException {
        fetcher.cleanup();

        verify(fetcher.closeable, never()).close();
    }

    @Test
    public void testHandlesExceptionOnClose() throws Exception {
        Closeable closeable = fetcher.loadData(Priority.NORMAL);

        doThrow(new IOException("Test")).when(closeable).close();
        fetcher.cleanup();
        verify(closeable).close();
    }

    private static class TestLocalUriFetcher extends LocalUriFetcher<Closeable> {
        final Closeable closeable = mock(Closeable.class);
        public TestLocalUriFetcher(Context context, Uri uri) {
            super(context, uri);
        }

        @Override
        protected Closeable loadResource(Uri uri, ContentResolver contentResolver) throws FileNotFoundException {
            return closeable;
        }

        @Override
        protected void close(Closeable data) throws IOException {
            data.close();
        }
    }
}
