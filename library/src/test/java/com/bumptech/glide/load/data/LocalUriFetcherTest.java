package com.bumptech.glide.load.data;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.Priority;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class LocalUriFetcherTest {
  private TestLocalUriFetcher fetcher;
  @Mock DataFetcher.DataCallback<Closeable> callback;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    fetcher = new TestLocalUriFetcher(RuntimeEnvironment.application, Uri.parse("content://empty"));
  }

  @Test
  public void testClosesDataOnCleanup() throws Exception {
    fetcher.loadData(Priority.NORMAL, callback);
    fetcher.cleanup();

    verify(fetcher.closeable).close();
  }

  @Test
  public void testDoesNotCloseNullData() throws IOException {
    fetcher.cleanup();

    verify(fetcher.closeable, never()).close();
  }

  @Test
  public void testHandlesExceptionOnClose() throws Exception {
    fetcher.loadData(Priority.NORMAL, callback);

    doThrow(new IOException("Test")).when(fetcher.closeable).close();
    fetcher.cleanup();
    verify(fetcher.closeable).close();
  }

  private static class TestLocalUriFetcher extends LocalUriFetcher<Closeable> {
    final Closeable closeable = mock(Closeable.class);

    public TestLocalUriFetcher(Context context, Uri uri) {
      super(context.getContentResolver(), uri);
    }

    @Override
    protected Closeable loadResource(Uri uri, ContentResolver contentResolver)
        throws FileNotFoundException {
      return closeable;
    }

    @Override
    protected void close(Closeable data) throws IOException {
      data.close();
    }

    @Override
    public Class<Closeable> getDataClass() {
      return Closeable.class;
    }
  }
}
