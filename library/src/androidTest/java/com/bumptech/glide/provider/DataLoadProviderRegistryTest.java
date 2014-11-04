package com.bumptech.glide.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DataLoadProviderRegistryTest {
    private DataLoadProviderRegistry factory;

    @Before
    public void setUp() {
        factory = new DataLoadProviderRegistry();
    }

    @Test
    public void testCanRegisterAndRetrieveDataLoadProvider() {
        DataLoadProvider<Object, Object> provider = mock(DataLoadProvider.class);
        factory.register(Object.class, Object.class, provider);

        assertEquals(provider, factory.get(Object.class, Object.class));
    }

    @Test
    public void testReturnsEmptyProviderIfNoneIsRegistered() {
        DataLoadProvider<Object, Object> loadProvider = factory.get(Object.class, Object.class);
        assertNotNull(loadProvider);

        assertNull(loadProvider.getCacheDecoder());
        assertNull(loadProvider.getSourceDecoder());
        assertNull(loadProvider.getEncoder());
    }
}
