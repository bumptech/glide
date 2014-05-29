package com.bumptech.glide.request;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MultiTypeRequestCoordinatorTest {
    private MultiTypeHarness harness;

    @Before
    public void setUp() {
        harness = new MultiTypeHarness();
    }

    @Test
    public void testAllRequestsAreStartedOnRun() {
        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();
        coordinator.run();

        for (Request request : harness.requests) {
            verify(request).run();
        }
    }

    @Test
    public void testIfARequestCompletesSynchronouslyNoOthersAreRun() {
        Request first = mock(Request.class);
        Request second = mock(Request.class);

        when(first.isComplete()).thenReturn(true);

        harness.setRequests(first, second);
        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();
        coordinator.run();

        verify(first).run();
        verify(second, never()).run();
    }

    @Test
    public void testAllRequestsAreClearedOnClear() {
        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();
        coordinator.clear();

        for (Request request : harness.requests) {
            verify(request).clear();
        }
    }

    @Test
    public void testNotCompleteIfNoRequestsAreComplete() {
        for (Request request : harness.requests) {
            when(request.isComplete()).thenReturn(false);
        }

        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();
        assertFalse(coordinator.isAnyRequestComplete());
        assertFalse(coordinator.isComplete());
    }

    @Test
    public void testIsCompleteIfAnyRequestIsComplete() {
        when(harness.requests[0].isComplete()).thenReturn(true);
        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();

        assertTrue(coordinator.isAnyRequestComplete());
        assertTrue(coordinator.isComplete());
    }

    @Test
    public void testIsCompleteIfParentIsComplete() {
        harness.parent = mock(RequestCoordinator.class);
        when(harness.parent.isAnyRequestComplete()).thenReturn(true);

        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();

        assertTrue(coordinator.isAnyRequestComplete());
    }

    @Test
    public void testCanSetImageIfNoRequestsAreComplete() {
        for (Request request : harness.requests) {
            when(request.isComplete()).thenReturn(false);
        }

        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();
        for (Request request : harness.requests) {
            assertTrue(coordinator.canSetImage(request));
        }
    }

    @Test
    public void testCanNotSetImageIfParentIsNonNullAndReturnsFalse() {
        harness.parent = mock(RequestCoordinator.class);

        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();

        when(harness.parent.canSetImage(eq(coordinator))).thenReturn(false);

        for (Request request : harness.requests) {
            assertFalse(coordinator.canSetImage(request));
        }
    }

    @Test
    public void testCanSetPlaceholderIfFirstRequestAndNoRequestsComplete() {
        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();

        assertTrue(coordinator.canSetPlaceholder(harness.requests[0]));
        for (int i = 1; i < harness.requests.length; i++) {
            assertFalse(coordinator.canSetPlaceholder(harness.requests[i]));
        }
    }

    @Test
    public void testCanNotSetPlaceholderIfParentNonNullAndReturnsFalse() {
        harness.parent = mock(RequestCoordinator.class);

        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();

        when(harness.parent.canSetPlaceholder(eq(coordinator))).thenReturn(false);

        for (Request request : harness.requests) {
            assertFalse(coordinator.canSetPlaceholder(request));
        }
    }

    @Test
    public void testCanNotSetPlaceholderIfFirstRequestAndRequestsComplete() {
        when(harness.requests[1].isComplete()).thenReturn(true);

        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();

        assertFalse(coordinator.canSetPlaceholder(harness.requests[0]));
    }

    @Test
    public void testCanSetPlaceholderIfAllRequestsFailed() {
        for (Request request : harness.requests) {
            when(request.isFailed()).thenReturn(true);
        }

        MultiTypeRequestCoordinator requestCoordinator = harness.getCoordinator();

        for (Request request : harness.requests) {
            assertTrue(requestCoordinator.canSetPlaceholder(request));
        }
    }

    @Test
    public void testIsNotFailedIfNotAllRequestsFailed() {
        when(harness.requests[0].isFailed()).thenReturn(true);

        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();

        assertFalse(coordinator.isFailed());
    }

    @Test
    public void testIsFailedWhenAllRequestsFailed() {
        for (Request request : harness.requests) {
            when(request.isFailed()).thenReturn(true);
        }

        MultiTypeRequestCoordinator coordinator = harness.getCoordinator();

        assertTrue(coordinator.isFailed());
    }

    private static class MultiTypeHarness {
        Request[] requests = new Request[] { mock(Request.class), mock(Request.class), mock(Request.class)};
        RequestCoordinator parent = null;
        MultiTypeRequestCoordinator coordinator;

        public MultiTypeRequestCoordinator getCoordinator() {
            coordinator = new MultiTypeRequestCoordinator(parent);
            coordinator.setRequests(requests);
            return coordinator;
        }

        public void setRequests(Request... requests) {
            this.requests = requests;
        }
    }
}
