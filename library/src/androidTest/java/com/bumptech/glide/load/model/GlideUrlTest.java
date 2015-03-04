package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.MalformedURLException;
import java.net.URL;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GlideUrlTest {

    @Test(expected = IllegalArgumentException.class)
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
    public void testCanCompareHashcodeOfGlideUrlsCreatedWithDifferentTypes() throws MalformedURLException {
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
        final String original =  "http://www.commitstrip.com/wp-content/uploads/2014/07/"
                + "Excel-\u00E0-toutes-les-sauces-650-finalenglish.jpg";

        final String escaped = "http://www.commitstrip.com/wp-content/uploads/2014/07/"
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
}