package com.bumptech.glide.request;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThumbnailRequestCoordinatorTest {
    private Request full;
    private Request thumb;
    private ThumbnailRequestCoordinator coordinator;

    @Before
    public void setUp() {
        full = mock(Request.class);
        thumb = mock(Request.class);
        coordinator = new ThumbnailRequestCoordinator();
        coordinator.setRequests(full, thumb);
    }

    @Test
    public void testIsRunningIsFalseIfNeitherRequestIsRunning() {
        assertFalse(coordinator.isRunning());
    }

    @Test
    public void testIsRunningIsTrueIfFullIsRunning() {
        when(full.isRunning()).thenReturn(true);
        assertTrue(coordinator.isRunning());
    }

    @Test
    public void testIsNotRunningIfFullIsNotRunningButThumbIs() {
        when(full.isRunning()).thenReturn(false);
        when(thumb.isRunning()).thenReturn(true);
        assertFalse(coordinator.isRunning());
    }

    @Test
    public void testStartsFullOnRunIfNotRunning() {
        when(full.isRunning()).thenReturn(false);
        coordinator.begin();

        verify(full).begin();
    }

    @Test
    public void testStartsThumbOnRunIfNotRunning() {
        when(thumb.isRunning()).thenReturn(false);
        coordinator.begin();

        verify(thumb).begin();
    }

    @Test
    public void testDoesNotStartFullOnRunIfRunning() {
        when(full.isRunning()).thenReturn(true);
        coordinator.begin();

        verify(full, never()).begin();
    }

    @Test
    public void testDoesNotStartThumbOnRunIfRunning() {
        when(thumb.isRunning()).thenReturn(true);
        coordinator.begin();

        verify(thumb, never()).begin();
    }

    @Test
    public void testDoesNotAllowThumbToSetPlaceholder() {
        assertFalse(coordinator.canNotifyStatusChanged(thumb));
    }

    @Test
    public void testAllowsFullToSetPlaceholder() {
        assertTrue(coordinator.canNotifyStatusChanged(full));
    }

    @Test
    public void testDoesNotAllowFullToSetPlaceholderIfThumbComplete() {
        when(thumb.isComplete()).thenReturn(true);
        assertFalse(coordinator.canNotifyStatusChanged(full));
    }

    @Test
    public void testCallsClearOnRequestsWhenCleared() {
        coordinator.clear();
        InOrder order = inOrder(thumb, full);
        order.verify(thumb).clear();
        order.verify(full).clear();
    }

    @Test
    public void testRecyclesRequestsWhenRecycled() {
        coordinator.recycle();
        verify(thumb).recycle();
        verify(full).recycle();
    }

}
