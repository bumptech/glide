package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.engine.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LazyBitmapDrawableResourceTest {
  @Mock private Resource<Bitmap> bitmapResource;
  private LazyBitmapDrawableResource resource;
  private Resources resources;
  private Bitmap bitmap;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(bitmapResource.get()).thenReturn(bitmap);

    resources = ApplicationProvider.getApplicationContext().getResources();
    resource =
        (LazyBitmapDrawableResource) LazyBitmapDrawableResource.obtain(resources, bitmapResource);
  }

  @Test
  public void obtain_withNullBitmapResource_returnsNull() {
    assertThat(LazyBitmapDrawableResource.obtain(resources, null)).isNull();
  }

  @Test
  public void getSize_returnsSizeOfWrappedResource() {
    when(bitmapResource.getSize()).thenReturn(100);
    assertThat(resource.getSize()).isEqualTo(100);
  }

  @Test
  public void recycle_callsRecycleOnWrappedResource() {
    resource.recycle();
    verify(bitmapResource).recycle();
  }

  @Test
  public void recycle_doesNotRecycleWrappedBitmap() {
    resource.recycle();
    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void get_returnsDrawableContainingWrappedBitmap() {
    BitmapDrawable drawable = resource.get();
    assertThat(drawable.getBitmap()).isSameInstanceAs(bitmap);
  }

  @Test
  public void initialize_withNonInitializableResource_doesNothing() {
    resource.initialize();
  }

  @Test
  public void initialize_withWrappedInitializableResource_callsInitializeOnWrapped() {
    InitializableBitmapResource bitmapResource = mock(InitializableBitmapResource.class);
    resource =
        (LazyBitmapDrawableResource) LazyBitmapDrawableResource.obtain(resources, bitmapResource);
    resource.initialize();

    verify(bitmapResource).initialize();
  }

  private interface InitializableBitmapResource extends Initializable, Resource<Bitmap> {
    // Intentionally empty.
  }
}
