package com.bumptech.glide.resize;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class DefaultResourceReferenceCounterTest {
    private DefaultResourceReferenceCounter referenceCounter;

    @Before
    public void setUp() {
        referenceCounter = new DefaultResourceReferenceCounter();
    }

    @Test
    public void testResourceCanBeAcquiredAndReleased() {
        Resource resource = mock(Resource.class);
        referenceCounter.acquireResource(resource);
        referenceCounter.releaseResource(resource);

        verify(resource).recycle();
    }

    @Test
    public void testResourceIsNotRecycledIfReferencesRemain() {
        Resource resource = mock(Resource.class);

        referenceCounter.acquireResource(resource);
        referenceCounter.acquireResource(resource);
        referenceCounter.releaseResource(resource);

        verify(resource, never()).recycle();
    }

    @Test
    public void testResourceIsRecycledIfMultipleReferencesAreAcquiredAndAllAreReleased() {
        Resource resource = mock(Resource.class);

        referenceCounter.acquireResource(resource);
        referenceCounter.acquireResource(resource);
        referenceCounter.releaseResource(resource);
        referenceCounter.acquireResource(resource);
        referenceCounter.releaseResource(resource);
        referenceCounter.releaseResource(resource);

        verify(resource).recycle();
    }

}
