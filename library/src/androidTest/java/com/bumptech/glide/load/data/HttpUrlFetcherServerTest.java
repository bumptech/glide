package com.bumptech.glide.load.data;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.model.GlideUrl;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

/**
 * Tests {@link com.bumptech.glide.load.data.HttpUrlFetcher} against server responses. Tests for behavior
 * (connection/disconnection/options) should go in {@link com.bumptech.glide.load.data.HttpUrlFetcherTest}, response
 * handling should go here.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class HttpUrlFetcherServerTest {
    private static final String DEFAULT_PATH = "/fakepath";

    private MockWebServer mockWebServer;
    private boolean defaultFollowRedirects;

    @Before
    public void setUp() throws IOException {
        defaultFollowRedirects = HttpURLConnection.getFollowRedirects();
        HttpURLConnection.setFollowRedirects(false);
        mockWebServer = new MockWebServer();
        mockWebServer.play();
    }

    @After
    public void tearDown() throws IOException {
        HttpURLConnection.setFollowRedirects(defaultFollowRedirects);
        mockWebServer.shutdown();
    }

    @Test
    public void testReturnsInputStreamOnStatusOk() throws Exception {
        String expected = "fakedata";
        mockWebServer.enqueue(new MockResponse()
                .setBody(expected)
                .setResponseCode(200));
        HttpUrlFetcher fetcher = getFetcher();
        InputStream is = fetcher.loadData(Priority.HIGH);
        assertThat(isToString(is), equalTo(expected));
    }

    @Test
    public void testHandlesRedirect301s() throws Exception {
        String expected = "fakedata";
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(301)
            .setHeader("Location", mockWebServer.getUrl("/redirect")));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(expected));
        InputStream is = getFetcher().loadData(Priority.LOW);
        assertThat(isToString(is), equalTo(expected));
    }

    @Test
    public void testHandlesRedirect302s() throws Exception {
        String expected = "fakedata";
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(302)
            .setHeader("Location", mockWebServer.getUrl("/redirect")));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(expected));
        InputStream is = getFetcher().loadData(Priority.LOW);
        assertThat(isToString(is), equalTo(expected));
    }

    @Test
    public void testHandlesRelativeRedirects() throws Exception {
        String expected = "fakedata";
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(301)
            .setHeader("Location", "/redirect"));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(expected));
        InputStream is = getFetcher().loadData(Priority.NORMAL);
        assertThat(isToString(is), equalTo(expected));

        mockWebServer.takeRequest();
        RecordedRequest second = mockWebServer.takeRequest();
        assertThat(second.getPath(), endsWith("/redirect"));
    }

    @Test
    public void testHandlesUpToFiveRedirects() throws Exception {
        int numRedirects = 4;
        String expected = "redirectedData";
        String redirectBase = "/redirect";
        for (int i = 0; i < numRedirects; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(301)
                    .setHeader("Location", mockWebServer.getUrl(redirectBase + i)));
        }
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200).setBody(expected));

        InputStream is = getFetcher().loadData(Priority.NORMAL);
        assertThat(isToString(is), equalTo(expected));

        assertThat(mockWebServer.takeRequest().getPath(), containsString(DEFAULT_PATH));
        for (int i = 0; i < numRedirects; i++) {
            assertThat(mockWebServer.takeRequest().getPath(), containsString(redirectBase + i));
        }
    }

    @Test
    public void testThrowsOnRedirectLoops() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(301).setHeader("Location", mockWebServer.getUrl("/redirect")));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(301).setHeader("Location", mockWebServer.getUrl("/redirect")));

        try {
            getFetcher().loadData(Priority.IMMEDIATE);
            fail("Didn't get expected IOException");
        } catch (SocketTimeoutException e) {
            fail("Didn't expect SocketTimeoutException");
        } catch (IOException e) {
            // Expected.
        }
    }

    @Test
    public void testThrowsIfRedirectLocationIsNotPresent() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(301));

        try {
            getFetcher().loadData(Priority.NORMAL);
            fail("Didn't get expected IOException");
        } catch (IOException e) {
            // Expected.
        }
    }

    @Test
    public void testThrowsIfRedirectLocationIsPresentAndEmpty() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(301).setHeader("Location", ""));

        try {
            getFetcher().loadData(Priority.NORMAL);
            fail("Didn't get expected IOException");
        } catch (IOException e) {
            // Expected.
        }
    }

    @Test(expected = IOException.class)
    public void testThrowsIfStatusCodeIsNegativeOne() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(-1));
        getFetcher().loadData(Priority.LOW);
    }

    @Test(expected = IOException.class)
    public void testThrowsAfterTooManyRedirects() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(301)
                    .setHeader("Location", mockWebServer.getUrl("/redirect" + i)));
        }
        getFetcher().loadData(Priority.NORMAL);
    }

    @Test(expected = IOException.class)
    public void testThrowsIfStatusCodeIs500() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        getFetcher().loadData(Priority.NORMAL);
    }

    @Test(expected = IOException.class)
    public void testThrowsIfStatusCodeIs400() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));
        getFetcher().loadData(Priority.LOW);
    }

    @Test(expected = SocketTimeoutException.class)
    public void testSetsReadTimeout() throws Exception {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.enqueue(new MockResponse().setBody("test").throttleBody(1, 2501, TimeUnit.SECONDS));
        mockWebServer.play();
        try {
            getFetcher().loadData(Priority.HIGH);
        } finally {
            mockWebServer.shutdown();
        }
    }

    private HttpUrlFetcher getFetcher() {
        URL url = mockWebServer.getUrl(DEFAULT_PATH);
        return new HttpUrlFetcher(new GlideUrl(url));
    }

    private static String isToString(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }
        return new String(os.toByteArray());
    }
}
