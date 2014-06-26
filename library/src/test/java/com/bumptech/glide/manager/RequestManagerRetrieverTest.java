package com.bumptech.glide.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import com.bumptech.glide.RequestManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RequestManagerRetrieverTest {
    private static final String PARENT_TAG = "parent";
    private RetrieverHarness[] harnesses;

    @Before
    public void setUp() {
        harnesses = new RetrieverHarness[] { new DefaultRetrieverHarness(), new SupportRetrieverHarness() };

        // If we don't pause the looper, fragment transactions are executed synchronously which is not how they would
        // normally behave.
        ShadowLooper shadowLooper = Robolectric.shadowOf(Looper.getMainLooper());
        shadowLooper.pause();
    }

    @Test
    public void testCreatesNewFragmentIfNoneExists() {
        for (RetrieverHarness harness : harnesses) {
            harness.doGet();

            assertTrue(harness.hasFragmentWithTag(RequestManagerRetriever.TAG));
        }
    }

    @Test
    public void testReturnsNewManagerIfNoneExists() {
        for (RetrieverHarness harness : harnesses) {
            assertNotNull(harness.doGet());
        }
    }

    @Test
    public void testReturnsExistingRequestManagerIfExists() {
        for (RetrieverHarness harness : harnesses) {
            RequestManager requestManager = mock(RequestManager.class);

            harness.addFragmentWithTag(RequestManagerRetriever.TAG, requestManager);

            assertEquals(requestManager, harness.doGet());
        }
    }

    @Test
    public void testReturnsNewRequestManagerIfFragmentExistsButHasNoRequestManager() {
        for (RetrieverHarness harness : harnesses) {
            harness.addFragmentWithTag(RequestManagerRetriever.TAG, null);

            assertNotNull(harness.doGet());
        }
    }

    @Test
    public void testSavesNewRequestManagerToFragmentIfCreatesRequestManagerForExistingFragment() {
        for (RetrieverHarness harness : harnesses) {
            harness.addFragmentWithTag(RequestManagerRetriever.TAG, null);
            RequestManager first = harness.doGet();
            RequestManager second = harness.doGet();

            assertEquals(first, second);
        }
    }

    @Test
    public void testHasValidTag() {
        assertEquals(RequestManagerRetriever.class.getPackage().getName(), RequestManagerRetriever.TAG);
    }

    @Test
    public void testCanGetRequestManagerFromActivity() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().get();
        RequestManager manager = RequestManagerRetriever.get(activity);
        assertEquals(manager, RequestManagerRetriever.get(activity));
    }

    @Test
    public void testSupportCanGetRequestManagerFromActivity() {
        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).create().start().get();
        RequestManager manager = RequestManagerRetriever.get(fragmentActivity);
        assertEquals(manager, RequestManagerRetriever.get(fragmentActivity));
    }

    @Test
    public void testCanGetRequestManagerFromFragment() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
        android.app.Fragment fragment = new android.app.Fragment();
        activity.getFragmentManager()
                .beginTransaction()
                .add(fragment, PARENT_TAG)
                .commit();
        activity.getFragmentManager().executePendingTransactions();

        RequestManager manager = RequestManagerRetriever.get(fragment);
        assertEquals(manager, RequestManagerRetriever.get(fragment));
    }

    @Test
    public void testSupportCanGetRequestManagerFromFragment() {
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).create().start().resume().get();
        Fragment fragment = new Fragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment, PARENT_TAG)
                .commit();
        activity.getSupportFragmentManager().executePendingTransactions();

        RequestManager manager = RequestManagerRetriever.get(fragment);
        assertEquals(manager, RequestManagerRetriever.get(fragment));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfFragmentNotAttached() {
        android.app.Fragment fragment = new android.app.Fragment();
        RequestManagerRetriever.get(fragment);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfSupportFragmentNotAttached() {
        Fragment fragment = new Fragment();
        RequestManagerRetriever.get(fragment);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfActivityDestroyed() {
        DefaultRetrieverHarness harness = new DefaultRetrieverHarness();
        harness.getController().pause().stop().destroy();
        harness.doGet();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfFragmentActivityDestroyed() {
        SupportRetrieverHarness harness = new SupportRetrieverHarness();
        harness.getController().pause().stop().destroy();
        harness.doGet();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenNullContext() {
        RequestManagerRetriever.get((Context) null);
    }

    @Test
    public void testChecksIfContextIsFragmentActivity() {
        SupportRetrieverHarness harness = new SupportRetrieverHarness();
        RequestManager requestManager = harness.doGet();

        assertEquals(requestManager, RequestManagerRetriever.get((Context) harness.getController().get()));
    }

    @Test
    public void testChecksIfContextIsActivity() {
        DefaultRetrieverHarness harness = new DefaultRetrieverHarness();
        RequestManager requestManager = harness.doGet();

        assertEquals(requestManager, RequestManagerRetriever.get((Context) harness.getController().get()));
    }

    @Test
    public void testReturnsNonNullManagerIfGivenApplicationContext() {
        assertNotNull(RequestManagerRetriever.get(Robolectric.application));
    }

    private interface RetrieverHarness {

        public ActivityController getController();

        public RequestManager doGet();

        public boolean hasFragmentWithTag(String tag);

        public void addFragmentWithTag(String tag, RequestManager manager);
    }

    public class DefaultRetrieverHarness implements RetrieverHarness {
        private final ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class);
        private final android.app.Fragment parent;

        public DefaultRetrieverHarness() {
            this.parent = new android.app.Fragment();

            controller.create();
            controller.get().getFragmentManager()
                .beginTransaction()
                .add(parent, PARENT_TAG)
                .commitAllowingStateLoss();
            controller.get().getFragmentManager().executePendingTransactions();
            controller.start().resume();
        }

        @Override
        public ActivityController getController() {
            return controller;
        }

        @Override
        public RequestManager doGet() {
            return RequestManagerRetriever.get(controller.get());
        }

        @Override
        public boolean hasFragmentWithTag(String tag) {
            return controller.get().getFragmentManager().findFragmentByTag(RequestManagerRetriever.TAG) != null;
        }

        @Override
        public void addFragmentWithTag(String tag, RequestManager requestManager) {
            RequestManagerFragment fragment = new RequestManagerFragment();
            fragment.setRequestManager(requestManager);
            controller.get().getFragmentManager()
                    .beginTransaction()
                    .add(fragment, RequestManagerRetriever.TAG)
                    .commitAllowingStateLoss();
            controller.get().getFragmentManager().executePendingTransactions();
        }
    }

    public class SupportRetrieverHarness implements RetrieverHarness {
        private final ActivityController<FragmentActivity> controller = Robolectric.buildActivity(
                FragmentActivity.class);
        private final Fragment parent;

        public SupportRetrieverHarness() {
            this.parent = new Fragment();

            controller.create();
            controller.get().getSupportFragmentManager()
                    .beginTransaction()
                    .add(parent, PARENT_TAG)
                    .commitAllowingStateLoss();
            controller.get().getSupportFragmentManager().executePendingTransactions();
            controller.start().resume();
        }

        @Override
        public ActivityController getController() {
            return controller;
        }

        @Override
        public RequestManager doGet() {
            return RequestManagerRetriever.get(controller.get());
        }

        @Override
        public boolean hasFragmentWithTag(String tag) {
            return controller.get().getSupportFragmentManager().findFragmentByTag(RequestManagerRetriever.TAG)
                    != null;
        }

        @Override
        public void addFragmentWithTag(String tag, RequestManager manager) {
            SupportRequestManagerFragment fragment = new SupportRequestManagerFragment();
            fragment.setRequestManager(manager);
            controller.get().getSupportFragmentManager()
                    .beginTransaction()
                    .add(fragment, RequestManagerRetriever.TAG)
                    .commitAllowingStateLoss();
            controller.get().getSupportFragmentManager().executePendingTransactions();
        }
    }
}
