package com.bumptech.glide.load.model.stream;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.HttpUrlFetcher;
import com.bumptech.glide.load.model.GlideUrl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.InputStream;

@RunWith(JUnit4.class)
public class HttpGlideUrlLoaderTest {
  private HttpGlideUrlLoader loader;
  private GlideUrl model;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    loader = new HttpGlideUrlLoader();
    model = mock(GlideUrl.class);
  }

  @Test
  public void testReturnsValidFetcher() {
    DataFetcher<InputStream> result = loader.buildLoadData(model, 100, 100, new Options()).fetcher;
    assertThat(result).isInstanceOf(HttpUrlFetcher.class);
  }
}
