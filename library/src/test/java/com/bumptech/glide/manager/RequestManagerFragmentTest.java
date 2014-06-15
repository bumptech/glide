package com.bumptech.glide.manager;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RequestManagerFragmentTest {
    private static final String TAG = "tag";
    private Harness[] harnesses;

    @Before
    public void setUp() {
        harnesses = new Harness[] {
            new RequestManagerHarness(),
            new SupportRequestManagerHarness()
        };
    }

    @Test
    public void testSupportCanSetAndGetRequestManager() {
        for (Harness harness : harnesses) {
            LifecycleRequestManager manager = mock(LifecycleRequestManager.class);
            harness.setRequestManager(manager);
            assertEquals(manager, harness.getManager());
        }
    }

    @Test
    public void testCallsRequestManagerStart() {
        for (Harness harness : harnesses) {
            harness.getController().start();

            verify(harness.getManager()).onStart();
        }
    }

    @Test
    public void testIgnoresNullRequestManagerOnStart() {
        for (Harness harness : harnesses) {
            harness.setRequestManager(null);
            harness.getController().start();
        }
    }

    @Test
    public void testCallsRequestManagerStop() {
        for (Harness harness : harnesses) {
            harness.getController().start().resume().pause().stop();

            verify(harness.getManager()).onStop();
        }
    }

    @Test
    public void testIgnoresNullRequestManagerOnStop() {
        for (Harness harness : harnesses) {
            harness.setRequestManager(null);
            harness.getController().start().stop();
        }
    }

    @Test
    public void testCallsRequestManagerDestroy() {
        for (Harness harness : harnesses) {
            harness.getController().start().resume().pause().stop().destroy();

            verify(harness.getManager()).onDestroy();
        }
    }

    @Test
    public void testIgnoresNullRequestManagerOnDestroy() {
        for (Harness harness : harnesses) {
            harness.setRequestManager(null);
            harness.getController().start().resume().pause().stop().destroy();
        }
    }

    private interface Harness {
        public LifecycleRequestManager getManager();

        public void setRequestManager(LifecycleRequestManager manager);

        public ActivityController getController();
    }

    private static class RequestManagerHarness implements Harness {
        private final ActivityController<Activity> controller;
        private final RequestManagerFragment fragment;
        private final LifecycleRequestManager manager;

        public RequestManagerHarness() {
            fragment = new RequestManagerFragment();
            controller = Robolectric.buildActivity(Activity.class).create();
            controller.get()
                .getFragmentManager()
                .beginTransaction()
                .add(fragment, TAG)
                .commit();
            controller.get().getFragmentManager().executePendingTransactions();

            this.manager = mock(LifecycleRequestManager.class);
            fragment.setRequestManager(manager);
        }

        @Override
        public LifecycleRequestManager getManager() {
            return fragment.getRequestManager();
        }

        @Override
        public void setRequestManager(LifecycleRequestManager requestManager) {
            fragment.setRequestManager(requestManager);
        }

        @Override
        public ActivityController getController() {
            return controller;
        }
    }

    private static class SupportRequestManagerHarness implements Harness {
        private final SupportRequestManagerFragment supportFragment;
        private final ActivityController<FragmentActivity> supportController;
        private final LifecycleRequestManager manager;

        public SupportRequestManagerHarness() {
            supportFragment = new SupportRequestManagerFragment();
            supportController = Robolectric.buildActivity(FragmentActivity.class).create();

            supportController.get()
                .getSupportFragmentManager()
                .beginTransaction()
                .add(supportFragment, TAG)
                .commit();
            supportController.get().getSupportFragmentManager().executePendingTransactions();

            this.manager = mock(LifecycleRequestManager.class);
            supportFragment.setRequestManager(manager);

        }

        @Override
        public LifecycleRequestManager getManager() {
            return supportFragment.getRequestManager();
        }

        @Override
        public void setRequestManager(LifecycleRequestManager manager) {
            supportFragment.setRequestManager(manager);
        }

        @Override
        public ActivityController getController() {
            return supportController;
        }
    }
}
