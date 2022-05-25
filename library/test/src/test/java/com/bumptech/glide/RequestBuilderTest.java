package com.bumptech.glide;

import static com.bumptech.glide.tests.BackgroundUtil.testInBackground;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.widget.ImageView;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.SingleRequest;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.tests.BackgroundUtil.BackgroundTester;
import com.bumptech.glide.tests.TearDownGlide;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class RequestBuilderTest {
  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();

  @Mock private RequestListener<Object> listener1;
  @Mock private RequestListener<Object> listener2;
  @Mock private Target<Object> target;
  @Mock private GlideContext glideContext;
  @Mock private RequestManager requestManager;
  @Captor private ArgumentCaptor<SingleRequest<Object>> requestCaptor;
  private Glide glide;
  private Application context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    glide = Glide.get(ApplicationProvider.getApplicationContext());
    context = ApplicationProvider.getApplicationContext();
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfContextIsNull() {
    new RequestBuilder<>(null /*context*/, requestManager, Object.class, context);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsWhenTransitionsOptionsIsNull() {
    //noinspection ConstantConditions testing if @NonNull is enforced
    getNullModelRequest().transition(null);
  }

  @Test
  public void testDoesNotThrowWithNullModelWhenRequestIsBuilt() {
    getNullModelRequest().into(target);
  }

  @Test
  public void testAddsNewRequestToRequestTracker() {
    getNullModelRequest().into(target);

    verify(requestManager).track(eq(target), isA(Request.class));
  }

  @Test
  public void testRemovesPreviousRequestFromRequestTracker() {
    Request previous = mock(Request.class);
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
    final ImageView imageView = new ImageView(ApplicationProvider.getApplicationContext());
    testInBackground(
        new BackgroundTester() {
          @Override
          public void runTest() {
            getNullModelRequest().into(imageView);
          }
        });
  }

  @Test
  public void doesNotThrowIfIntoTargetCalledOnBackgroundThread() throws InterruptedException {
    final Target<Object> target = mock(Target.class);
    testInBackground(
        new BackgroundTester() {
          @Override
          public void runTest() {
            getNullModelRequest().into(target);
          }
        });
  }

  @Test
  public void testMultipleRequestListeners() {
    getNullModelRequest().addListener(listener1).addListener(listener2).into(target);
    verify(requestManager).track(any(Target.class), requestCaptor.capture());
    requestCaptor
        .getValue()
        .onResourceReady(
            new SimpleResource<>(new Object()),
            DataSource.LOCAL,
            /*isLoadedFromAlternateCacheKey=*/ false);

    verify(listener1)
        .onResourceReady(any(), any(), isA(Target.class), isA(DataSource.class), anyBoolean());
    verify(listener2)
        .onResourceReady(any(), any(), isA(Target.class), isA(DataSource.class), anyBoolean());
  }

  @Test
  public void testListenerApiOverridesListeners() {
    getNullModelRequest().addListener(listener1).listener(listener2).into(target);
    verify(requestManager).track(any(Target.class), requestCaptor.capture());
    requestCaptor
        .getValue()
        .onResourceReady(
            new SimpleResource<>(new Object()),
            DataSource.LOCAL,
            /*isLoadedFromAlternateCacheKey=*/ false);

    // The #listener API removes any previous listeners, so the first listener should not be called.
    verify(listener1, never())
        .onResourceReady(any(), any(), isA(Target.class), isA(DataSource.class), anyBoolean());
    verify(listener2)
        .onResourceReady(any(), any(), isA(Target.class), isA(DataSource.class), anyBoolean());
  }

  private RequestBuilder<Object> getNullModelRequest() {
    when(glideContext.buildImageViewTarget(isA(ImageView.class), isA(Class.class)))
        .thenReturn(mock(ViewTarget.class));
    when(glideContext.getDefaultRequestOptions()).thenReturn(new RequestOptions());
    when(requestManager.getDefaultRequestOptions()).thenReturn(new RequestOptions());
    when(requestManager.getDefaultTransitionOptions(any(Class.class)))
        .thenReturn(new GenericTransitionOptions<>());
    return new RequestBuilder<>(glide, requestManager, Object.class, context).load((Object) null);
  }
}
