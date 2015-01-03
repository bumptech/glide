package com.bumptech.glide.load.model;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GenericLoaderFactoryTest {

    GenericLoaderFactory genericFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        genericFactory = new GenericLoaderFactory(Robolectric.application);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCanHandleModelSubclasses() {
        ModelLoaderFactory<Uri, Object> factory = mock(ModelLoaderFactory.class);
        ModelLoader<Uri, Object> loader = mock(ModelLoader.class);
        when(factory.build(any(Context.class), any(GenericLoaderFactory.class))).thenReturn(loader);
        genericFactory.register(Uri.class, Object.class, factory);

        Uri stringUri = Uri.parse("content://fake");
        assertThat(genericFactory.buildModelLoaders(stringUri.getClass())).contains(loader);
    }
}