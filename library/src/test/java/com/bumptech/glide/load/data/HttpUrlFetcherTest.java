package com.bumptech.glide.load.data;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.model.GlideUrl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpUrlFetcherTest {
    private HttpURLConnection urlConnection;
    private HttpUrlFetcher fetcher;
    private GlideUrl glideUrl;

    @Before
    public void setUp() throws IOException {
        urlConnection =  mock(HttpURLConnection.class);
        URL url = new URL("http://www.google.com");
        HttpUrlFetcher.HttpUrlConnectionFactory factory = mock(HttpUrlFetcher.HttpUrlConnectionFactory.class);
        when(factory.build(eq(url))).thenReturn(urlConnection);
        glideUrl = mock(GlideUrl.class);
        when(glideUrl.toURL()).thenReturn(url);
        fetcher = new HttpUrlFetcher(glideUrl, factory);
        when(urlConnection.getResponseCode()).thenReturn(200);
    }

    @Test
    public void testReturnsModelAsString() {
        final String expected = "fakeId";
        when(glideUrl.toString()).thenReturn(expected);
        assertEquals(expected, fetcher.getId());
    }


    @Test
    public void testSetsReadTimeout() throws Exception {
        fetcher.loadData(Priority.HIGH);
        verify(urlConnection).setReadTimeout(eq(2500));
    }

    @Test
    public void testSetsConnectTimeout() throws Exception {
        fetcher.loadData(Priority.IMMEDIATE);
        verify(urlConnection).setConnectTimeout(eq(2500));
    }

    @Test
    public void testReturnsInputStreamOnStatusOk() throws Exception {
        InputStream expected = new ByteArrayInputStream(new byte[0]);
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getInputStream()).thenReturn(expected);

        assertEquals(expected, fetcher.loadData(Priority.NORMAL));
    }

    @Test(expected = IOException.class)
    public void testThrowsIfStatusCodeIsNegativeOne() throws Exception {
        when(urlConnection.getResponseCode()).thenReturn(-1);
        fetcher.loadData(Priority.HIGH);
    }

    @Test(expected = IOException.class)
    public void testThrowsIfStatusCodeIs300() throws Exception {
        when(urlConnection.getResponseCode()).thenReturn(300);
        fetcher.loadData(Priority.HIGH);
    }

    @Test(expected = IOException.class)
    public void testThrowsIfStatusCodeIs500() throws Exception {
        when(urlConnection.getResponseCode()).thenReturn(500);
        fetcher.loadData(Priority.HIGH);
    }

    @Test
    public void testReturnsNullIfCancelledBeforeConnects() throws Exception {
        InputStream notExpected = new ByteArrayInputStream(new byte[0]);
        when(urlConnection.getInputStream()).thenReturn(notExpected);

        fetcher.cancel();
        assertNull(fetcher.loadData(Priority.LOW));
    }

    @Test
    public void testDisconnectsUrlOnCleanup() throws Exception {
        fetcher.loadData(Priority.HIGH);
        fetcher.cleanup();

        verify(urlConnection).disconnect();
    }

    @Test
    public void testDoesNotThrowIfCleanupCalledBeforeStarted() {
        fetcher.cleanup();
    }

    @Test
    public void testDoesNotThrowIfCancelCalledBeforeStart() {
        fetcher.cancel();
    }

    @Test
    public void testCancelDoesNotDisconnectIfAlreadyConnected() throws Exception {
        fetcher.loadData(Priority.HIGH);
        fetcher.cancel();

        verify(urlConnection, never()).disconnect();
    }

    @Test
    public void testDisconnectsUrlConnectionOnCancelIfNotYetCancelled() throws IOException, InterruptedException {
        final CountDownLatch mainThreadLatch = new CountDownLatch(1);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                mainThreadLatch.countDown();
                countDownLatch.await();
                return null;
            }
        }).when(urlConnection).connect();

        Thread bg = new Thread() {
            @Override
            public void run() {
                try {
                    fetcher.loadData(Priority.HIGH);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        bg.start();

        mainThreadLatch.await();
        fetcher.cancel();
        countDownLatch.countDown();
        bg.join();

        verify(urlConnection).connect();
        verify(urlConnection).disconnect();
    }
}