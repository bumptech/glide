package com.bumptech.glide.load.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import com.bumptech.glide.load.Key;
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

/**
 * Tests for the {@link com.bumptech.glide.load.model.ResourceLoader} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class ResourceLoaderTest {

  @Mock ModelLoader<Uri, Object> uriLoader;
  @Mock DataFetcher<Object> fetcher;
  @Mock Key key;
  private Options options;

  private ResourceLoader<Object> loader;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    options = new Options();

    loader = new ResourceLoader<>(RuntimeEnvironment.application.getResources(), uriLoader);
  }

  @Test
  public void testCanHandleId() {
    int id = android.R.drawable.star_off;
    Uri contentUri = Uri.parse("android.resource://android/drawable/star_off");
    when(uriLoader.buildLoadData(eq(contentUri), anyInt(), anyInt(), any(Options.class)))
        .thenReturn(new ModelLoader.LoadData<>(key, fetcher));

    assertTrue(loader.handles(id));
    assertEquals(fetcher, loader.buildLoadData(id, 100, 100, new Options()).fetcher);
  }

    @Test
    public void testDoesNotThrowOnInvalidOrMissingId() {
      assertThat(loader.buildLoadData(1234, 0, 0, options)).isNull();
      verify(uriLoader, never()).buildLoadData(any(Uri.class), anyInt(), anyInt(),
          any(Options.class));
    }
}
