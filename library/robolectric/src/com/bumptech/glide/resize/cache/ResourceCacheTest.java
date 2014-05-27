package com.bumptech.glide.resize.cache;

import com.bumptech.glide.resize.Resource;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceCacheTest {

    @Test
    public void testResourceRemovedListenerIsNotifiedWhenResourceIsRemoved() {
        ResourceCache resourceCache = new ResourceCache(100);
        Resource resource = mock(Resource.class);
        when(resource.getSize()).thenReturn(200);

        ResourceCache.ResourceRemovedListener listener = mock(ResourceCache.ResourceRemovedListener.class);

        resourceCache.setResourceRemovedListener(listener);
        resourceCache.put("a", resource);

        verify(listener).onResourceRemoved(eq(resource));
    }

    @Test
    public void testSizeIsBasedOnResource() {
        ResourceCache resourceCache = new ResourceCache(100);
        Resource first = getResource(50);
        resourceCache.put("1", first);
        Resource second = getResource(50);
        resourceCache.put("2", second);

        assertTrue(resourceCache.contains("1"));
        assertTrue(resourceCache.contains("2"));

        Resource third = getResource(50);
        resourceCache.put("3", third);

        assertFalse(resourceCache.contains("1"));
        assertTrue(resourceCache.contains("2"));
        assertTrue(resourceCache.contains("3"));
    }

    private Resource getResource(int size) {
        Resource resource = mock(Resource.class);
        when(resource.getSize()).thenReturn(size);
        return resource;
    }
}
