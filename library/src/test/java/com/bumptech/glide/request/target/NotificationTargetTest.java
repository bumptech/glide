package com.bumptech.glide.request.target;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowNotificationManager;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18, shadows = NotificationTargetTest
    .UpdateShadowNotificationManager.class)
public class NotificationTargetTest {
  private UpdateShadowNotificationManager shadowManager;
  private RemoteViews remoteViews;
  private int viewId;
  private Notification notification;
  private int notificationId;
  private String notificationTag;
  private NotificationTarget target;

  @Before
  public void setUp() {
    NotificationManager notificationManager = (NotificationManager) RuntimeEnvironment.application
        .getSystemService(Context.NOTIFICATION_SERVICE);
    shadowManager = (UpdateShadowNotificationManager) ShadowExtractor.extract(notificationManager);

    remoteViews = mock(RemoteViews.class);
    viewId = 123;
    notification = mock(Notification.class);
    notificationId = 456;
    notificationTag = "tag";


    target =
        new NotificationTarget(RuntimeEnvironment.application, 100 /*width*/, 100 /*height*/,
            viewId, remoteViews, notification, notificationId, notificationTag);
  }

  @Test
  public void testSetsBitmapOnRemoteViewsWithGivenImageIdOnResourceReady() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    target.onResourceReady(bitmap, null /*glideAnimation*/);
    verify(remoteViews).setImageViewBitmap(eq(viewId), eq(bitmap));
  }

  @Test
  public void updatesNotificationManagerWithNotificationIdAndNotificationOnResourceReady() {
    target.onResourceReady(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), null
    /*glideAnimation*/);

    assertEquals(notificationId, shadowManager.updatedNotificationId);
    assertEquals(notificationTag, shadowManager.updatedNotificationTag);
    assertEquals(notification, shadowManager.updatedNotification);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfContextIsNull() {
    new NotificationTarget(null /*context*/, 100 /*width*/, 100 /*height*/,
        123 /*viewId*/, mock(RemoteViews.class), mock(Notification.class), 456 /*notificationId*/,
        "tag" /*notificationTag*/);
  }


  @Test(expected = NullPointerException.class)
  public void testThrowsIfNotificationIsNull() {
    new NotificationTarget(RuntimeEnvironment.application, 100 /*width*/, 100 /*height*/,
        123 /*viewId*/, mock(RemoteViews.class), null /*notification*/, 456 /*notificationId*/,
        "tag" /*notificationTag*/);
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfRemoteViewsIsNull() {
    new NotificationTarget(RuntimeEnvironment.application, 100 /*width*/, 100 /*height*/,
        123 /*viewId*/, null /*remoteViews*/, mock(Notification.class), 456 /*notificationId*/,
        "tag" /*notificationTag*/);
  }

  @Implements(NotificationManager.class)
  public static class UpdateShadowNotificationManager extends ShadowNotificationManager {
    int updatedNotificationId;
    String updatedNotificationTag;
    Notification updatedNotification;

    @Implementation
    public void notify(String notificationTag, int notificationId, Notification notification) {
      updatedNotificationTag = notificationTag;
      updatedNotificationId = notificationId;
      updatedNotification = notification;
    }
  }
}
