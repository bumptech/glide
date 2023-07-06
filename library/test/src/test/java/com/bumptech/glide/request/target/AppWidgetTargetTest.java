package com.bumptech.glide.request.target;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAppWidgetManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK, shadows = AppWidgetTargetTest.UpdateShadowAppWidgetManager.class)
public class AppWidgetTargetTest {
  private UpdateShadowAppWidgetManager shadowManager;
  private RemoteViews views;
  private int viewId;

  @Before
  public void setUp() {
    shadowManager =
        Shadow.extract(AppWidgetManager.getInstance(ApplicationProvider.getApplicationContext()));
    viewId = 1234;
    views = mock(RemoteViews.class);
  }

  @Test
  public void testSetsBitmapOnRemoteViewsWithViewIdWhenCreatedWithComponentName() {
    ComponentName componentName = mock(ComponentName.class);
    AppWidgetTarget target =
        new AppWidgetTarget(
            ApplicationProvider.getApplicationContext(), viewId, views, componentName);

    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    target.onResourceReady(bitmap, null /*glideAnimation*/);

    verify(views).setImageViewBitmap(eq(viewId), eq(bitmap));
  }

  @Test
  public void testUpdatesAppWidgetWhenCreatedWithComponentName() {
    ComponentName componentName = mock(ComponentName.class);
    AppWidgetTarget target =
        new AppWidgetTarget(
            ApplicationProvider.getApplicationContext(), viewId, views, componentName);

    target.onResourceReady(
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), null
        /*glideAnimation*/ );

    assertEquals(componentName, shadowManager.updatedComponentName);
    assertEquals(views, shadowManager.updatedRemoteViews);
  }

  @Test
  public void testSetsBitmapOnRemoteViewsWithViewIdWhenCreatedWithWidgetIds() {
    int[] widgetIds = new int[] {1};
    AppWidgetTarget target =
        new AppWidgetTarget(ApplicationProvider.getApplicationContext(), viewId, views, widgetIds);

    Bitmap bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);
    target.onResourceReady(bitmap, null /*glideAnimation*/);

    verify(views).setImageViewBitmap(eq(viewId), eq(bitmap));
  }

  @Test
  public void testUpdatesAppWidgetWhenCreatedWithWidgetIds() {
    int[] widgetIds = new int[] {1};
    AppWidgetTarget target =
        new AppWidgetTarget(ApplicationProvider.getApplicationContext(), viewId, views, widgetIds);

    target.onResourceReady(
        Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888), null
        /*glideAnimation*/ );

    assertThat(widgetIds).isEqualTo(shadowManager.updatedWidgetIds);
    assertEquals(views, shadowManager.updatedRemoteViews);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsWhenGivenNullContextWithWidgetIds() {
    new AppWidgetTarget(null /*context*/, 1234 /*viewId*/, views, 1 /*widgetIds*/);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsWhenGivenNullContextWithComponentName() {
    new AppWidgetTarget(null /*context*/, 1234 /*viewId*/, views, mock(ComponentName.class));
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsWhenGivenNullRemoteViewsWithWidgetIds() {
    new AppWidgetTarget(
        ApplicationProvider.getApplicationContext(), viewId, null /*remoteViews*/, 1 /*widgetIds*/);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsWhenGivenNullRemoteViewsWithComponentName() {
    new AppWidgetTarget(
        ApplicationProvider.getApplicationContext(),
        viewId,
        null /*remoteViews*/,
        mock(ComponentName.class));
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsWhenGivenNullWidgetIds() {
    new AppWidgetTarget(
        ApplicationProvider.getApplicationContext(), viewId, views, (int[]) null /*widgetIds*/);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsWhenGivenEmptyWidgetIds() {
    new AppWidgetTarget(ApplicationProvider.getApplicationContext(), viewId, views);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsWhenGivenNullComponentName() {
    new AppWidgetTarget(
        ApplicationProvider.getApplicationContext(), viewId, views, (ComponentName) null);
  }

  @Implements(AppWidgetManager.class)
  public static class UpdateShadowAppWidgetManager extends ShadowAppWidgetManager {
    int[] updatedWidgetIds;
    RemoteViews updatedRemoteViews;
    ComponentName updatedComponentName;

    @Implementation
    @Override
    public void updateAppWidget(int[] appWidgetIds, RemoteViews views) {
      updatedWidgetIds = appWidgetIds;
      updatedRemoteViews = views;
    }

    @Implementation
    public void updateAppWidget(ComponentName componentName, RemoteViews views) {
      updatedComponentName = componentName;
      updatedRemoteViews = views;
    }
  }
}
