package com.bumptech.glide.resize;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import org.junit.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static com.bumptech.glide.resize.RequestContext.DependencyNotFoundException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

    @Test
    public void testReturnsCanLoadIfHasRequiredDependencies() {
        RequestContext parent = new RequestContext();
        RequestContext requestContext = new RequestContext(parent);
        parent.register(mock(ModelLoader.class), Object.class, Object.class);
        requestContext.register(mock(ResourceDecoder.class), Object.class, Object.class);
        requestContext.register(mock(ResourceDecoder.class), Object.class);
        requestContext.register(mock(ResourceEncoder.class), Object.class);

        assertTrue(requestContext.canLoad(Object.class, Object.class, Object.class));
    }

    @Test
    public void testReturnsCanNotLoadIfMissingModelLoader() {
        RequestContext requestContext = new RequestContext();
        requestContext.register(mock(ResourceDecoder.class), Object.class, Object.class);
        requestContext.register(mock(ResourceDecoder.class), Object.class);
        requestContext.register(mock(ResourceEncoder.class), Object.class);

        assertFalse(requestContext.canLoad(Object.class, Object.class, Object.class));
    }

    @Test
    public void testReturnsCanNotLoadIfMissingDecoder() {
        RequestContext requestContext = new RequestContext();
        requestContext.register(mock(ModelLoader.class), Object.class, Object.class);
        requestContext.register(mock(ResourceDecoder.class), Object.class);
        requestContext.register(mock(ResourceEncoder.class), Object.class);

        assertFalse(requestContext.canLoad(Object.class, Object.class, Object.class));
    }

    @Test
    public void testReturnsCanNotLoadIfMissingCacheDecoder() {
        RequestContext requestContext = new RequestContext();
        requestContext.register(mock(ModelLoader.class), Object.class, Object.class);
        requestContext.register(mock(ResourceDecoder.class), Object.class, Object.class);
        requestContext.register(mock(ResourceEncoder.class), Object.class);

        assertFalse(requestContext.canLoad(Object.class, Object.class, Object.class));
    }

    @Test
    public void testReturnsCanNotLoadIfMissingEncoder() {
        RequestContext requestContext = new RequestContext();
        requestContext.register(mock(ModelLoader.class), Object.class, Object.class);
        requestContext.register(mock(ResourceDecoder.class), Object.class);
        requestContext.register(mock(ResourceDecoder.class), Object.class, Object.class);

        assertFalse(requestContext.canLoad(Object.class, Object.class, Object.class));
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsOnNullModel() {
        RequestContext requestContext = new RequestContext();
        requestContext.register((ModelLoader) null, Object.class, Object.class);
    }

    @Test(expected =  NullPointerException.class)
    public void testThrowsOnNullDecoder() {
        RequestContext requestContext = new RequestContext();
        requestContext.register((ResourceDecoder) null, Object.class, Object.class);
    }

    @Test(expected =  NullPointerException.class)
    public void testThrowsOnNullCacheDecoder() {
        RequestContext requestContext = new RequestContext();
        requestContext.register((ResourceDecoder) null, Object.class);
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsOnNullEncoder() {
        RequestContext requestContext = new RequestContext();
        requestContext.register((ResourceEncoder) null, Object.class);
    }
}
