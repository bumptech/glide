package com.bumptech.glide.integration.volley;

import android.os.SystemClock;
import com.android.volley.NoConnectionError;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

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
    public void testThrowsIfRedirectLocationIsEmpty() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(301));
        }

        try {
            getFetcher().loadData(Priority.NORMAL);
            fail("Didn't get expected IOException");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(VolleyError.class));
        }
    }

    @Test
    public void testThrowsIfStatusCodeIsNegativeOne() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(-1));
        try {
            getFetcher().loadData(Priority.LOW);
            fail("Failed to get expected exception");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(NoConnectionError.class));
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
            assertThat(e.getCause(), instanceOf(NoConnectionError.class));
            assertThat(e.getCause().getCause(), instanceOf(ProtocolException.class));
        }
    }


    @Test
    public void testThrowsIfStatusCodeIs500() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
        try {
            getFetcher().loadData(Priority.NORMAL);
            fail("Failed to get expected exception");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(ServerError.class));
        }
    }

    @Test
    public void testThrowsIfStatusCodeIs400() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("error"));
        try {
            getFetcher().loadData(Priority.LOW);
            fail("Failed to get expected exception");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(ServerError.class));
        }
    }

    private DataFetcher<InputStream> getFetcher() {
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
        return new VolleyStreamFetcher(requestQueue, url.toString(), requestFuture);
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
