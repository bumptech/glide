package com.bumptech.glide.load.model;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.net.Uri;
import com.bumptech.glide.load.Options;
import java.io.InputStream;
import java.net.MalformedURLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class UrlUriLoaderTest {
  private static final int IMAGE_SIDE = 100;
  private static final Options OPTIONS = new Options();

  @Mock private ModelLoader<GlideUrl, InputStream> urlLoader;
  private UrlUriLoader<InputStream> loader;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    loader = new UrlUriLoader<>(urlLoader);
  }

  @Test
  public void testHandlesHttpUris() throws MalformedURLException {
    Uri httpUri = Uri.parse("http://www.google.com");
    loader.buildLoadData(httpUri, IMAGE_SIDE, IMAGE_SIDE, OPTIONS);

    assertTrue(loader.handles(httpUri));
    verify(urlLoader)
        .buildLoadData(
            eq(new GlideUrl(httpUri.toString())), eq(IMAGE_SIDE), eq(IMAGE_SIDE), eq(OPTIONS));
  }

  @Test
  public void testHandlesHttpsUris() throws MalformedURLException {
    Uri httpsUri = Uri.parse("https://www.google.com");
    loader.buildLoadData(httpsUri, IMAGE_SIDE, IMAGE_SIDE, OPTIONS);

    assertTrue(loader.handles(httpsUri));
    verify(urlLoader)
        .buildLoadData(
            eq(new GlideUrl(httpsUri.toString())), eq(IMAGE_SIDE), eq(IMAGE_SIDE), eq(OPTIONS));
  }

  // Test for https://github.com/bumptech/glide/issues/71.
  @Test
  public void testHandlesMostlyInvalidHttpUris() {
    Uri mostlyInvalidHttpUri =
        Uri.parse(
            "http://myserver_url.com:80http://myserver_url.com/webapp/images/no_image.png"
                + "?size=100");

    assertTrue(loader.handles(mostlyInvalidHttpUri));
    loader.buildLoadData(mostlyInvalidHttpUri, IMAGE_SIDE, IMAGE_SIDE, OPTIONS);
    verify(urlLoader)
        .buildLoadData(
            eq(new GlideUrl(mostlyInvalidHttpUri.toString())),
            eq(IMAGE_SIDE),
            eq(IMAGE_SIDE),
            eq(OPTIONS));
  }
}
