package com.bumptech.glide.resize;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import org.junit.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static com.bumptech.glide.resize.RequestContext.DependencyNotFoundException;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class RequestContextTest {

    @Test
    public void testModelLoadersCanBeRegisteredAndRetrieved() {
        RequestContext requestContext = new RequestContext();

        ModelLoader<Object, InputStream> modelLoader = mock(ModelLoader.class);
        requestContext.register(modelLoader, Object.class, InputStream.class);

        assertEquals(modelLoader, requestContext.getModelLoader(Object.class, InputStream.class));
    }

    @Test
    public void testCacheDecoderCanBeRegisteredAndRetrieved() {
        RequestContext requestContext = new RequestContext();

        ResourceDecoder<InputStream, Object> cacheDecoder = mock(ResourceDecoder.class);
        requestContext.register(cacheDecoder, Object.class);

        assertEquals(cacheDecoder, requestContext.getCacheDecoder(Object.class));
    }

    @Test
    public void testDecoderCanBeRegisteredAndRetrieved() {
        RequestContext context = new RequestContext();

        ResourceDecoder<ByteBuffer, Bitmap> decoder = mock(ResourceDecoder.class);
        context.register(decoder, ByteBuffer.class, Bitmap.class);

        assertEquals(decoder, context.getDecoder(ByteBuffer.class, Bitmap.class));
    }

    @Test
    public void testEncoderCanBeRegisteredAndRetrieved() {
        RequestContext context = new RequestContext();

        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        context.register(encoder, Object.class);

        assertEquals(encoder, context.getEncoder(Object.class));
    }

    @Test(expected = DependencyNotFoundException.class)
    public void testThrowsIfNoDependenciesAreRegistered() {
        RequestContext requestContext = new RequestContext();

        requestContext.getGeneric(Object.class);
    }

    @Test(expected = DependencyNotFoundException.class)
    public void testThrowsIfSpecificDependencyIsMissing() {
        RequestContext requestContext = new RequestContext();
        Object toRegister = new Object();
        requestContext.registerGeneric(toRegister, Object.class);

        requestContext.getGeneric(Bitmap.class);
    }

    @Test
    public void testCanRegisterAndRetrieveGenericObject() {
        RequestContext requestContext = new RequestContext();
        Object toRegister = new Object();
        requestContext.registerGeneric(toRegister, Object.class);

        assertEquals(toRegister, requestContext.getGeneric(Object.class));
    }

    @Test
    public void testMissingDependenciesCanBeSatisfiedByParent() {
        RequestContext parent = new RequestContext();
        Object toRegister = new Object();
        parent.registerGeneric(toRegister, Object.class);

        RequestContext child = new RequestContext(parent);

        assertEquals(toRegister, child.getGeneric(Object.class));
    }

    @Test(expected = DependencyNotFoundException.class)
    public void testThrowsIfSpecificDependencyIsMissingFromParents() {
        RequestContext parent = new RequestContext();
        RequestContext child = new RequestContext(parent);

        child.getGeneric(Object.class);
    }
}
