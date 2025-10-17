package com.bumptech.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.LifecycleListener;
import com.bumptech.glide.manager.RequestManagerTreeNode;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.ResourceIds.raw;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class RequestManagerTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  @Mock private RequestManagerTreeNode treeNode;

  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private RequestManager requestManager;
  private Context context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();
    Glide glide = Glide.get(context);
    requestManager =
        new RequestManager(
            glide,
            new Lifecycle() {
              @Override
              public void addListener(@NonNull LifecycleListener listener) {
                listener.onStart();
              }

              @Override
              public void removeListener(@NonNull LifecycleListener listener) {
                // Do nothing.
              }
            },
            treeNode,
            context);
  }

  /** Tests #2262. */
  @Test
  public void clear_withNonOwningRequestManager_afterOwningManagerIsDestroyed_doesNotThrow() {
    // First destroy our Fragment/Activity RequestManager.
    requestManager.onDestroy();

    final ImageView imageView = new ImageView(context);
    imageView.measure(100, 100);
    imageView.layout(0, 0, 100, 100);
    // Then start a new load with our now destroyed RequestManager.
    concurrency.loadOnMainThread(requestManager.load(ResourceIds.raw.canonical), imageView);

    // Finally clear our new load with any RequestManager other than the one we used to start it.
    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.with(context).clear(imageView);
          }
        });
  }

  /** Tests b/69361054. */
  @Test
  public void clear_withNonOwningRequestManager_onBackgroundThread_doesNotThrow() {
    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            requestManager.onDestroy();
          }
        });

    final Target<Drawable> target = concurrency.wait(requestManager.load(raw.canonical).submit());

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.with(context).clear(target);
          }
        });
  }
}
