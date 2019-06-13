package com.bumptech.glide.load.resource.gif;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class GifDrawableResourceTest {
  private GifDrawable drawable;
  private GifDrawableResource resource;

  @Before
  public void setUp() {
    drawable = mock(GifDrawable.class);
    resource = new GifDrawableResource(drawable);
  }

  @Test
  public void testReturnsSizeFromDrawable() {
    final int size = 2134;
    when(drawable.getSize()).thenReturn(size);

    assertEquals(size, resource.getSize());
  }

  @Test
  public void testStopsAndThenRecyclesDrawableWhenRecycled() {
    resource.recycle();

    InOrder inOrder = inOrder(drawable);
    inOrder.verify(drawable).stop();
    inOrder.verify(drawable).recycle();
  }
}
