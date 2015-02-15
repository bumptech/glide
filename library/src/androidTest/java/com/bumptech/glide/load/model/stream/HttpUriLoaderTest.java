package com.bumptech.glide.load.model.stream;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import android.net.Uri;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.net.MalformedURLException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class HttpUriLoaderTest {
  private static final int IMAGE_SIDE = 100;

  @Mock
  ModelLoader<GlideUrl, InputStream> urlLoader;
  private HttpUriLoader loader;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    loader = new HttpUriLoader(urlLoader);
  }

  @Test
  public void testHandlesHttpUris() throws MalformedURLException {
    Uri httpUri = Uri.parse("http://www.google.com");
    loader.getDataFetcher(httpUri, IMAGE_SIDE, IMAGE_SIDE);

    assertTrue(loader.handles(httpUri));
    verify(urlLoader)
        .getDataFetcher(eq(new GlideUrl(httpUri.toString())), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
  }

  @Test
  public void testHandlesHttpsUris() throws MalformedURLException {
    Uri httpsUri = Uri.parse("https://www.google.com");
    loader.getDataFetcher(httpsUri, IMAGE_SIDE, IMAGE_SIDE);

    assertTrue(loader.handles(httpsUri));
    verify(urlLoader)
        .getDataFetcher(eq(new GlideUrl(httpsUri.toString())), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
  }

  // Test for https://github.com/bumptech/glide/issues/71.
  @Test
  public void testHandlesMostlyInvalidHttpUris() {
    Uri mostlyInvalidHttpUri = Uri.parse(
        "http://myserver_url.com:80http://myserver_url.com/webapp/images/no_image.png?size=100");

    assertTrue(loader.handles(mostlyInvalidHttpUri));
    loader.getDataFetcher(mostlyInvalidHttpUri, IMAGE_SIDE, IMAGE_SIDE);
    verify(urlLoader)
        .getDataFetcher(eq(new GlideUrl(mostlyInvalidHttpUri.toString())), eq(IMAGE_SIDE),
            eq(IMAGE_SIDE));
  }
}