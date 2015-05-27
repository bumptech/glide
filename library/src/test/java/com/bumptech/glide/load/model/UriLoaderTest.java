package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

/**
 * Tests for the {@link UriLoader} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class UriLoaderTest {
  // Not a magic number, just arbitrary non zero.
  private static final int IMAGE_SIDE = 120;

  @Mock DataFetcher<Object> localUriFetcher;
  @Mock UriLoader.LocalUriFetcherFactory<Object> factory;
  private UriLoader<Object> loader;
  private Options options;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    options = new Options();
    loader = new UriLoader<>(RuntimeEnvironment.application, factory);
  }

  @Test
  public void testHandlesFileUris() throws IOException {
    Uri fileUri = Uri.fromFile(new File("f"));
    when(factory.build(anyContext(), eq(fileUri))).thenReturn(localUriFetcher);

    assertTrue(loader.handles(fileUri));
    assertEquals(localUriFetcher,
        loader.buildLoadData(fileUri, IMAGE_SIDE, IMAGE_SIDE, options).fetcher);
  }

  @Test
  public void testHandlesResourceUris() throws IOException {
    Uri resourceUri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
    when(factory.build(anyContext(), eq(resourceUri))).thenReturn(localUriFetcher);

    assertTrue(loader.handles(resourceUri));
    assertEquals(localUriFetcher,
        loader.buildLoadData(resourceUri, IMAGE_SIDE, IMAGE_SIDE, options).fetcher);
  }

  @Test
  public void testHandlesContentUris() {
    Uri contentUri = Uri.parse("content://com.bumptech.glide");
    when(factory.build(anyContext(), eq(contentUri))).thenReturn(localUriFetcher);

    assertTrue(loader.handles(contentUri));
    assertEquals(localUriFetcher, loader.buildLoadData(contentUri, IMAGE_SIDE, IMAGE_SIDE,
        options).fetcher);
  }

  private static Context anyContext() {
    return any(Context.class);
  }
}
