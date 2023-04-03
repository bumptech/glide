package com.bumptech.glide.load.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.util.Preconditions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for the {@link com.bumptech.glide.load.model.ResourceLoader} class. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class ResourceLoaderTest {

  @Mock private ModelLoader<Uri, Object> uriLoader;
  @Mock private DataFetcher<Object> fetcher;
  @Mock private Key key;
  private Options options;

  private ResourceLoader<Object> loader;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    options = new Options();

    loader =
        new ResourceLoader<>(ApplicationProvider.getApplicationContext().getResources(), uriLoader);
  }

  @Test
  public void testCanHandleId() {
    int id = android.R.drawable.star_off;
    Uri contentUri = Uri.parse("android.resource://android/drawable/star_off");
    when(uriLoader.buildLoadData(eq(contentUri), anyInt(), anyInt(), any(Options.class)))
        .thenReturn(new ModelLoader.LoadData<>(key, fetcher));

    assertTrue(loader.handles(id));
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(loader.buildLoadData(id, 100, 100, new Options())).fetcher);
  }

  @Test
  public void testDoesNotThrowOnInvalidOrMissingId() {
    assertThat(loader.buildLoadData(1234, 0, 0, options)).isNull();
    verify(uriLoader, never())
        .buildLoadData(any(Uri.class), anyInt(), anyInt(), any(Options.class));
  }
}
