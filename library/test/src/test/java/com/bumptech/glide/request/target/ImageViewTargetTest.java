package com.bumptech.glide.request.target;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.request.transition.Transition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class ImageViewTargetTest {

  @Mock private AnimatedDrawable animatedDrawable;
  private ImageView view;
  private TestTarget target;
  private ColorDrawable drawable;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    view = new ImageView(ApplicationProvider.getApplicationContext());
    target = new TestTarget(view);
    drawable = new ColorDrawable(Color.RED);
  }

  @Test
  public void testReturnsCurrentDrawable() {
    view.setImageDrawable(drawable);

    assertEquals(drawable, target.getCurrentDrawable());
  }

  @Test
  public void testSetsDrawableSetsDrawableOnView() {
    target.setDrawable(drawable);

    assertEquals(drawable, view.getDrawable());
  }

  @Test
  public void testSetsDrawableOnLoadStarted() {
    target.onLoadStarted(drawable);

    assertEquals(drawable, view.getDrawable());
  }

  @Test
  public void testSetDrawableOnLoadFailed() {
    target.onLoadFailed(drawable);

    assertEquals(drawable, view.getDrawable());
  }

  @Test
  public void testSetsDrawableOnLoadCleared() {
    target.onLoadCleared(drawable);

    assertEquals(drawable, view.getDrawable());
  }

  @Test
  public void testSetsDrawableOnViewInOnResourceReadyWhenAnimationReturnsFalse() {
    @SuppressWarnings("unchecked")
    Transition<Drawable> animation = mock(Transition.class);
    when(animation.transition(any(Drawable.class), eq(target))).thenReturn(false);
    Drawable resource = new ColorDrawable(Color.GRAY);
    target.onResourceReady(resource, animation);

    assertEquals(resource, target.resource);
  }

  @Test
  public void testDoesNotSetDrawableOnViewInOnResourceReadyWhenAnimationReturnsTrue() {
    Drawable resource = new ColorDrawable(Color.RED);
    @SuppressWarnings("unchecked")
    Transition<Drawable> animation = mock(Transition.class);
    when(animation.transition(eq(resource), eq(target))).thenReturn(true);
    target.onResourceReady(resource, animation);

    assertNull(target.resource);
  }

  @Test
  public void testProvidesCurrentPlaceholderToAnimationIfPresent() {
    Drawable placeholder = new ColorDrawable(Color.BLACK);
    view.setImageDrawable(placeholder);

    @SuppressWarnings("unchecked")
    Transition<Drawable> animation = mock(Transition.class);

    target.onResourceReady(new ColorDrawable(Color.GREEN), animation);

    ArgumentCaptor<Drawable> drawableCaptor = ArgumentCaptor.forClass(Drawable.class);
    verify(animation).transition(drawableCaptor.capture(), eq(target));
    assertThat(((ColorDrawable) drawableCaptor.getValue()).getColor()).isEqualTo(Color.GREEN);
  }

  @Test
  public void onResourceReady_withAnimatableResource_startsAnimatableAfterSetResource() {
    AnimatedDrawable drawable = mock(AnimatedDrawable.class);
    ImageView view = mock(ImageView.class);
    target = new TestTarget(view);
    target.onResourceReady(drawable, /* transition= */ null);

    InOrder order = inOrder(view, drawable);
    order.verify(view).setImageDrawable(drawable);
    order.verify(drawable).start();
  }

  @Test
  public void onLoadCleared_withAnimatableDrawable_stopsDrawable() {
    target.onResourceReady(animatedDrawable, /* transition= */ null);
    verify(animatedDrawable).start();
    verify(animatedDrawable, never()).stop();

    target.onLoadCleared(/* placeholder= */ null);

    verify(animatedDrawable).stop();
  }

  private abstract static class AnimatedDrawable extends Drawable implements Animatable {
    // Intentionally empty.
  }

  private static final class TestTarget extends ImageViewTarget<Drawable> {
    public Drawable resource;

    TestTarget(ImageView view) {
      super(view);
    }

    @Override
    protected void setResource(Drawable resource) {
      this.resource = resource;
      view.setImageDrawable(resource);
    }
  }
}
