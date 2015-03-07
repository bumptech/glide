package com.bumptech.glide;

import static com.bumptech.glide.tests.BackgroundUtil.testInBackground;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorModelLoader;
import com.bumptech.glide.load.model.stream.StreamByteArrayLoader;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.manager.ConnectivityMonitor;
import com.bumptech.glide.manager.ConnectivityMonitor.ConnectivityListener;
import com.bumptech.glide.manager.ConnectivityMonitorFactory;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestManagerTreeNode;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.tests.BackgroundUtil;
import com.bumptech.glide.tests.GlideShadowLooper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = GlideShadowLooper.class)
public class RequestManagerTest {
    private RequestManager manager;
    private ConnectivityMonitor connectivityMonitor;
    private RequestTracker requestTracker;
    private ConnectivityListener connectivityListener;
    private RequestManager.DefaultOptions options;
    private Lifecycle lifecycle = mock(Lifecycle.class);
    private RequestManagerTreeNode treeNode = mock(RequestManagerTreeNode.class);

    @Before
    public void setUp() {
        connectivityMonitor = mock(ConnectivityMonitor.class);
        ConnectivityMonitorFactory factory = mock(ConnectivityMonitorFactory.class);
        when(factory.build(any(Context.class), any(ConnectivityMonitor.ConnectivityListener.class)))
                .thenAnswer(new Answer<ConnectivityMonitor>() {
                    @Override
                    public ConnectivityMonitor answer(InvocationOnMock invocation) throws Throwable {
                        connectivityListener = (ConnectivityListener) invocation.getArguments()[1];
                        return connectivityMonitor;
                    }
                });
        requestTracker = mock(RequestTracker.class);
        manager =
            new RequestManager(Robolectric.application, lifecycle, treeNode, requestTracker, factory);
        options = mock(RequestManager.DefaultOptions.class);
        manager.setDefaultOptions(options);
    }

    @Test
    public void testAppliesDefaultOptionsWhenUsingGenericModelLoaderAndDataClass() {
        Float model = 1f;
        ModelLoader<Float, InputStream> modelLoader = mock(ModelLoader.class);
        GenericTranscodeRequest<Float, InputStream, Bitmap> builder = manager.using(modelLoader, InputStream.class)
                .load(model)
                .as(Bitmap.class);

        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsWhenUsingImageStreamModelLoader() {
        String model = "fake";
        StreamModelLoader<String> modelLoader = mock(StreamModelLoader.class);
        DrawableTypeRequest<String> builder = manager.using(modelLoader)
                .load(model);

        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsWhenUsingByteArrayLoader() {
        byte[] model = new byte[] { 1, 4, 65, 2};
        StreamByteArrayLoader loader = mock(StreamByteArrayLoader.class);
        DrawableTypeRequest<byte[]> builder = manager.using(loader)
                .load(model);

        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsWhenUsingVideoFileDescriptorModelLoader() {
        String model = "fake";
        FileDescriptorModelLoader<String> modelLoader = mock(FileDescriptorModelLoader.class);
        DrawableTypeRequest<String> builder = manager.using(modelLoader)
                .load(model);

        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsToLoadString() {
        String model = "fake";
        DrawableTypeRequest<String> builder = manager.load(model);
        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsToLoadUri() {
        Uri uri = Uri.EMPTY;
        DrawableTypeRequest<Uri> builder = manager.load(uri);
        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsToLoadMediaStoreUri() {
        Uri uri = Uri.EMPTY;
        DrawableTypeRequest<Uri> builder = manager.loadFromMediaStore(uri, "image/jpeg", 123L, 0);

        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsToLoadResourceId() {
        int id = 123;
        DrawableTypeRequest<Integer> builder = manager.load(id);

        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsToLoadGenericFromImage() {
        ModelLoaderFactory<Double, InputStream> factory = mock(ModelLoaderFactory.class);
        when(factory.build(any(Context.class), any(GenericLoaderFactory.class))).thenReturn(mock(ModelLoader.class));
        Glide.get(Robolectric.application).register(Double.class, InputStream.class, factory);
        Double model = 2.2;
        DrawableTypeRequest<Double> builder = manager.load(model);

        verify(options).apply(eq(builder));
        Glide.get(Robolectric.application).unregister(Double.class, InputStream.class);
    }

    @Test
    public void testAppliesDefaultOptionsToLoadUrl() throws MalformedURLException {
        URL url = new URL("http://www.google.com");
        DrawableTypeRequest<URL> builder = manager.load(url);

        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsToLoadFromImageByteWithId() {
        byte[] model = new byte[] { 1, 2, 4 };
        DrawableTypeRequest<byte[]> builder = manager.load(model, "fakeId");

        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsToLoadFromImageBytes() {
        byte[] model = new byte[] { 5, 9, 23 };
        DrawableTypeRequest<byte[]> builder = manager.load(model);

        verify(options).apply(eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsToLoadGenericFromVideo() {
        ModelLoaderFactory<Float, InputStream> factory = mock(ModelLoaderFactory.class);
        when(factory.build(any(Context.class), any(GenericLoaderFactory.class))).thenReturn(mock(ModelLoader.class));
        Glide.get(Robolectric.application).register(Float.class, InputStream.class, factory);
        Float model = 23.2f;
        DrawableTypeRequest<Float> builder = manager.load(model);

        verify(options).apply(eq(builder));
        Glide.get(Robolectric.application).unregister(Float.class, InputStream.class);
    }

    @Test
    public void testPauseRequestsPausesRequests() {
        manager.pauseRequests();

        verify(requestTracker).pauseRequests();
    }

    @Test
    public void testResumeRequestsResumesRequests() {
        manager.resumeRequests();

        verify(requestTracker).resumeRequests();
    }

    @Test
    public void testPausesRequestsOnStop() {
        manager.onStart();
        manager.onStop();

        verify(requestTracker).pauseRequests();
    }

    @Test
    public void testResumesRequestsOnStart() {
        manager.onStart();

        verify(requestTracker).resumeRequests();
    }

    @Test
    public void testClearsRequestsOnDestroy() {
        manager.onDestroy();

        verify(requestTracker).clearRequests();
    }

    @Test
    public void testAddsConnectivityMonitorToLifecycleWhenConstructed() {
        verify(lifecycle).addListener(eq(connectivityMonitor));
    }

    @Test
    public void testAddsSelfToLifecycleWhenConstructed() {
        verify(lifecycle).addListener(eq(manager));
    }

    @Test
    public void testRestartsRequestOnConnected() {
        connectivityListener.onConnectivityChanged(true);

        verify(requestTracker).restartRequests();
    }

    @Test
    public void testDoesNotRestartRequestsOnDisconnected() {
        connectivityListener.onConnectivityChanged(false);

        verify(requestTracker, never()).restartRequests();
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsIfResumeCalledOnBackgroundThread() throws InterruptedException {
        testInBackground(new BackgroundUtil.BackgroundTester() {
            @Override
            public void runTest() throws Exception {
                manager.resumeRequests();
            }
        });
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsIfPauseCalledOnBackgroundThread() throws InterruptedException {
        testInBackground(new BackgroundUtil.BackgroundTester() {
            @Override
            public void runTest() throws Exception {
                manager.pauseRequests();
            }
        });
    }

    @Test
    public void testDelegatesIsPausedToRequestTracker() {
        when(requestTracker.isPaused()).thenReturn(true);
        assertTrue(manager.isPaused());
        when(requestTracker.isPaused()).thenReturn(false);
        assertFalse(manager.isPaused());
    }
}
