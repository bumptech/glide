package com.bumptech.glide.load.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
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

        assertEquals(expected, glideUrl.toString());
    }
}