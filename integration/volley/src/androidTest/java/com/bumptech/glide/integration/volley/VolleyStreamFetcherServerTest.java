package com.bumptech.glide.integration.volley;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.SystemClock;

import com.android.volley.NoConnectionError;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import com.bumptech.glide.testutil.TestUtil;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowSystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Tests {@link com.bumptech.glide.integration.volley.VolleyStreamFetcher} against server responses.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = VolleyStreamFetcherServerTest.FakeSystemClock.class)
public class VolleyStreamFetcherServerTest {
    private static final String DEFAULT_PATH = "/fakepath";

    private MockWebServer mockWebServer;
    private RequestQueue requestQueue;

    @Before
    public void setUp() throws IOException {
        requestQueue = Volley.newRequestQueue(Robolectric.application);
        mockWebServer = new MockWebServer();
        mockWebServer.play();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        requestQueue.stop();
    }

    @Test
    public void testReturnsInputStreamOnStatusOk() throws Exception {
        String expected = "fakedata";
        mockWebServer.enqueue(new MockResponse()
                .setBody(expected)
                .setResponseCode(200));
        DataFetcher<InputStream> fetcher = getFetcher();
        InputStream is = fetcher.loadData(Priority.HIGH);
        assertEquals(expected, TestUtil.isToString(is));
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
        assertEquals(expected, TestUtil.isToString(is));
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
        assertEquals(expected, TestUtil.isToString(is));
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
        assertEquals(expected, TestUtil.isToString(is));

        assertThat(mockWebServer.takeRequest().getPath()).contains(DEFAULT_PATH);
        for (int i = 0; i < numRedirects; i++) {
            assertThat(mockWebServer.takeRequest().getPath()).contains(redirectBase + i);
        }
    }

    @Test
    public void testThrowsIfRedirectLocationIsEmpty() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(301));
        }

        try {
            getFetcher().loadData(Priority.NORMAL);
            fail("Didn't get expected IOException");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(VolleyError.class);
        }
    }

    @Test
    public void testThrowsIfStatusCodeIsNegativeOne() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(-1));
        try {
            getFetcher().loadData(Priority.LOW);
            fail("Failed to get expected exception");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(NoConnectionError.class);
        }
    }

    @Test
    public void testThrowsAfterTooManyRedirects() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(301)
                    .setHeader("Location", mockWebServer.getUrl("/redirect" + i)));
        }
        try {
            getFetcher().loadData(Priority.NORMAL);
            fail("Failed to get expected exception");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(NoConnectionError.class);
            assertThat(e.getCause().getCause()).isInstanceOf(ProtocolException.class);
        }
    }


    @Test
    public void testThrowsIfStatusCodeIs500() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
        try {
            getFetcher().loadData(Priority.NORMAL);
            fail("Failed to get expected exception");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ServerError.class);
        }
    }

    @Test
    public void testThrowsIfStatusCodeIs400() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("error"));
        try {
            getFetcher().loadData(Priority.LOW);
            fail("Failed to get expected exception");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ServerError.class);
        }
    }

    @Test
    public void testAppliesHeadersInGlideUrl() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      String headerField = "field";
      String headerValue = "value";
      Map<String, String> headersMap = new HashMap<String, String>();
      headersMap.put(headerField, headerValue);
      Headers headers = mock(Headers.class);
      when(headers.getHeaders()).thenReturn(headersMap);

      getFetcher(headers).loadData(Priority.HIGH);

      assertThat(mockWebServer.takeRequest().getHeader(headerField)).isEqualTo(headerValue);
    }

    private DataFetcher<InputStream> getFetcher() {
      return getFetcher(Headers.NONE);
    }

    private DataFetcher<InputStream> getFetcher(Headers headers) {
        URL url = mockWebServer.getUrl(DEFAULT_PATH);
        VolleyRequestFuture<InputStream> requestFuture = new VolleyRequestFuture<InputStream>() {
            @Override
            public InputStream get() throws InterruptedException, ExecutionException {
                for (int i = 0; i < 251 && !isDone(); i++) {
                    Thread.sleep(10);
                    Robolectric.runUiThreadTasks();
                }
                if (!isDone()) {
                    fail("Failed to get response from Volley in time");
                }
                return super.get();
            }
        };
        return new VolleyStreamFetcher(requestQueue, new GlideUrl(url.toString(), headers), requestFuture);
    }

    /** A shadow clock that doesn't rely on running on an Android thread with a Looper. */
    @Implements(SystemClock.class)
    public static class FakeSystemClock extends ShadowSystemClock {

        @Implementation
        public static long elapsedRealtime() {
            // The default is to return something using the main looper, which doesn't exist on Volley's threads.
            return System.currentTimeMillis();
        }
    }
}
