package com.bumptech.glide.load.model;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.google.common.testing.EqualsTester;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class GlideUrlTest {

  @Test(expected = NullPointerException.class)
  public void testThrowsIfGivenURLIsNull() {
    new GlideUrl((URL) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfGivenStringUrlIsNull() {
    new GlideUrl((String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfGivenStringURLIsEmpty() {
    new GlideUrl("");
  }

  @Test
  public void testCanCompareGlideUrlsCreatedWithDifferentTypes() throws MalformedURLException {
    String stringUrl = "http://www.google.com";
    URL url = new URL(stringUrl);

    assertEquals(new GlideUrl(stringUrl), new GlideUrl(url));
  }

  @Test
  public void testCanCompareHashcodeOfGlideUrlsCreatedWithDifferentTypes()
      throws MalformedURLException {
    String stringUrl = "http://nytimes.com";
    URL url = new URL(stringUrl);

    assertEquals(new GlideUrl(stringUrl).hashCode(), new GlideUrl(url).hashCode());
  }

  @Test
  public void testProducesEquivalentUrlFromString() throws MalformedURLException {
    String stringUrl = "http://www.google.com";
    GlideUrl glideUrl = new GlideUrl(stringUrl);

    URL url = glideUrl.toURL();
    assertEquals(stringUrl, url.toString());
  }

  @Test
  public void testProducesEquivalentStringFromURL() throws MalformedURLException {
    String expected = "http://www.washingtonpost.com";
    URL url = new URL(expected);
    GlideUrl glideUrl = new GlideUrl(url);

    assertEquals(expected, glideUrl.toStringUrl());
  }

  @Test
  public void testIssue133() throws MalformedURLException {
    // u00e0=à
    final String original =
        "http://www.commitstrip.com/wp-content/uploads/2014/07/"
            + "Excel-\u00E0-toutes-les-sauces-650-finalenglish.jpg";

    final String escaped =
        "http://www.commitstrip.com/wp-content/uploads/2014/07/"
            + "Excel-%C3%A0-toutes-les-sauces-650-finalenglish.jpg";

    GlideUrl glideUrlFromString = new GlideUrl(original);
    assertEquals(escaped, glideUrlFromString.toURL().toString());

    GlideUrl glideUrlFromEscapedString = new GlideUrl(escaped);
    assertEquals(escaped, glideUrlFromEscapedString.toURL().toString());

    GlideUrl glideUrlFromUrl = new GlideUrl(new URL(original));
    assertEquals(escaped, glideUrlFromUrl.toURL().toString());

    GlideUrl glideUrlFromEscapedUrl = new GlideUrl(new URL(escaped));
    assertEquals(escaped, glideUrlFromEscapedUrl.toURL().toString());
  }

  @Test
  public void issue_2583() throws MalformedURLException {
    String original =
        "http://api.met.no/weatherapi/weathericon/1.1/?symbol=9;content_type=image/png";

    GlideUrl glideUrl = new GlideUrl(original);
    assertThat(glideUrl.toURL().toString()).isEqualTo(original);
    assertThat(glideUrl.toStringUrl()).isEqualTo(original);
  }

  @Test
  public void testEquals() throws MalformedURLException {
    Headers headers = mock(Headers.class);
    Headers otherHeaders = mock(Headers.class);
    String url = "http://www.google.com";
    String otherUrl = "http://mail.google.com";
    new EqualsTester()
        .addEqualityGroup(
            new GlideUrl(url),
            new GlideUrl(url),
            new GlideUrl(new URL(url)),
            new GlideUrl(new URL(url)))
        .addEqualityGroup(new GlideUrl(otherUrl), new GlideUrl(new URL(otherUrl)))
        .addEqualityGroup(new GlideUrl(url, headers), new GlideUrl(new URL(url), headers))
        .addEqualityGroup(new GlideUrl(url, otherHeaders), new GlideUrl(new URL(url), otherHeaders))
        .testEquals();
  }
}
