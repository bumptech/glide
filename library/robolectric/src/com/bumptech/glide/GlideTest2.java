package com.bumptech.glide;

import com.bumptech.glide.loader.bitmap.model.stream.StreamModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.Metadata;
import com.bumptech.glide.resize.target.Target;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GlideTest2 {
    private StreamModelLoader modelLoader;

    @Before
    public void setUp() throws Exception {
        byte[] data = new byte[0];
        InputStream is = new ByteArrayInputStream(data);

        ResourceFetcher resourceFetcher = mock(ResourceFetcher.class);
        when(resourceFetcher.loadResource(any(Metadata.class))).thenReturn(is);

        modelLoader = mock(StreamModelLoader.class);
        when(modelLoader.getResourceFetcher(any(Object.class), anyInt(), anyInt()))
                .thenReturn(resourceFetcher);
    }


    @Test
    public void testSomething() {
        Target target = mock(Target.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Target.SizeReadyCallback cb = (Target.SizeReadyCallback) invocation.getArguments()[0];
                cb.onSizeReady(100, 100);

                return null;
            }
        }).when(target).getSize(any(Target.SizeReadyCallback.class));

        Glide.with(Robolectric.application)
                .using(modelLoader)
                .load(new Object())
                .into(target);
        Robolectric.runUiThreadTasks();
        Robolectric.runBackgroundTasks();
        Robolectric.runUiThreadTasks();

        verify(target).getSize(any(Target.SizeReadyCallback.class));
    }
}
