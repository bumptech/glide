package com.bumptech.glide.request.target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.glide.request.transition.Transition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class ImageViewTargetTest {

  private ImageView view;
  private TestTarget target;
  private ColorDrawable drawable;

  @Before
  public void setUp() {
    view = new ImageView(RuntimeEnvironment.application);
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
    @SuppressWarnings("unchecked") Transition<Drawable> animation = mock(Transition.class);
    when(animation.transition(any(Drawable.class), eq(target))).thenReturn(false);
    Drawable resource = new ColorDrawable(Color.GRAY);
    target.onResourceReady(resource, animation);

    assertEquals(resource, target.resource);
  }

  @Test
  public void testDoesNotSetDrawableOnViewInOnResourceReadyWhenAnimationReturnsTrue() {
    Drawable resource = new ColorDrawable(Color.RED);
    @SuppressWarnings("unchecked") Transition<Drawable> animation = mock(Transition.class);
    when(animation.transition(eq(resource), eq(target))).thenReturn(true);
    target.onResourceReady(resource, animation);

    assertNull(target.resource);
  }

  @Test
  public void testProvidesCurrentPlaceholderToAnimationIfPresent() {
    Drawable placeholder = new ColorDrawable(Color.BLACK);
    view.setImageDrawable(placeholder);

    @SuppressWarnings("unchecked") Transition<Drawable> animation = mock(Transition.class);

    target.onResourceReady(new ColorDrawable(Color.GREEN), animation);

    verify(animation).transition(eq(placeholder), eq(target));
  }

  private static class TestTarget extends ImageViewTarget<Drawable> {
    public Drawable resource;

    public TestTarget(ImageView view) {
      super(view);
    }

    @Override
    protected void setResource(Drawable resource) {
      this.resource = resource;
    }
  }
}
