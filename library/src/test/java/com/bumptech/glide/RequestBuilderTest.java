package com.bumptech.glide;

import static com.bumptech.glide.tests.BackgroundUtil.testInBackground;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.widget.ImageView;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.tests.BackgroundUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class RequestBuilderTest {
  @Mock GlideContext glideContext;
  @Mock RequestManager requestManager;
  private Glide glide;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    glide = Glide.get(RuntimeEnvironment.application);
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfContextIsNull() {
    new RequestBuilder<>(null /*context*/, requestManager, Object.class);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsWhenTransitionsOptionsIsNull() {
    //noinspection ConstantConditions testing if @NonNull is enforced
    getNullModelRequest().transition(null);
  }

  @Test
  public void testDoesNotThrowWithNullModelWhenRequestIsBuilt() {
    getNullModelRequest().into(mock(Target.class));
  }

  @Test
  public void testAddsNewRequestToRequestTracker() {
    Target<Object> target = mock(Target.class);
    getNullModelRequest().into(target);

    verify(requestManager).track(eq(target), isA(Request.class));
  }

  @Test
  public void testRemovesPreviousRequestFromRequestTracker() {
    Request previous = mock(Request.class);
    Target<Object> target = mock(Target.class);
    when(target.getRequest()).thenReturn(previous);

    getNullModelRequest().into(target);

    verify(requestManager).clear(eq(target));
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfGivenNullTarget() {
    //noinspection ConstantConditions testing if @NonNull is enforced
    getNullModelRequest().into((Target<Object>) null);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfGivenNullView() {
    getNullModelRequest().into((ImageView) null);
  }

  @Test(expected = RuntimeException.class)
  public void testThrowsIfIntoViewCalledOnBackgroundThread() throws InterruptedException {
    final ImageView imageView = new ImageView(RuntimeEnvironment.application);
    testInBackground(new BackgroundUtil.BackgroundTester() {
      @Override
      public void runTest() throws Exception {
        getNullModelRequest().into(imageView);

      }
    });
  }

  @Test(expected = RuntimeException.class)
  public void testThrowsIfIntoTargetCalledOnBackgroundThread() throws InterruptedException {
    final Target<Object> target = mock(Target.class);
    testInBackground(new BackgroundUtil.BackgroundTester() {
      @Override
      public void runTest() throws Exception {
        getNullModelRequest().into(target);
      }
    });
  }

  private RequestBuilder<Object> getNullModelRequest() {
    when(glideContext.buildImageViewTarget(isA(ImageView.class), isA(Class.class)))
        .thenReturn(mock(Target.class));
    when(glideContext.getDefaultRequestOptions()).thenReturn(new RequestOptions());
    when(requestManager.getDefaultRequestOptions())
        .thenReturn((RequestOptions) new RequestOptions());
    return new RequestBuilder<>(glide, requestManager, Object.class)
        .load((Object) null);
  }
}
