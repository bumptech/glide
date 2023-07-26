package com.bumptech.glide.request.target;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Preconditions;

/**
 * This class is used to display downloaded Bitmap inside an ImageView of a Notification through
 * RemoteViews.
 *
 * <p>Note - For cancellation to work correctly, you must pass in the same instance of this class
 * for every subsequent load.
 */
// Public API.
@SuppressWarnings({"WeakerAccess", "unused"})
public class NotificationTarget extends CustomTarget<Bitmap> {
  private final RemoteViews remoteViews;
  private final Context context;
  private final int notificationId;
  private final String notificationTag;
  private final Notification notification;
  private final int viewId;

  /**
   * Constructor using a Notification object and a notificationId to get a handle on the
   * Notification in order to update it that uses {@link #SIZE_ORIGINAL} as the target width and
   * height.
   *
   * @param context Context to use in the AppWidgetManager initialization.
   * @param viewId The id of the ImageView view that will load the image.
   * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
   * @param notification The Notification object that we want to update.
   * @param notificationId The notificationId of the Notification that we want to load the Bitmap.
   */
  @SuppressLint("InlinedApi")
  // Alert users of Glide to have this permission.
  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  public NotificationTarget(
      Context context,
      int viewId,
      RemoteViews remoteViews,
      Notification notification,
      int notificationId) {
    this(context, viewId, remoteViews, notification, notificationId, null);
  }

  /**
   * Constructor using a Notification object, a notificationId, and a notificationTag to get a
   * handle on the Notification in order to update it that uses {@link #SIZE_ORIGINAL} as the target
   * width and height.
   *
   * @param context Context to use in the AppWidgetManager initialization.
   * @param viewId The id of the ImageView view that will load the image.
   * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
   * @param notification The Notification object that we want to update.
   * @param notificationId The notificationId of the Notification that we want to load the Bitmap.
   * @param notificationTag The notificationTag of the Notification that we want to load the Bitmap.
   *     May be {@code null}.
   */
  @SuppressLint("InlinedApi")
  // Alert users of Glide to have this permission.
  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  public NotificationTarget(
      Context context,
      int viewId,
      RemoteViews remoteViews,
      Notification notification,
      int notificationId,
      String notificationTag) {
    this(
        context,
        SIZE_ORIGINAL,
        SIZE_ORIGINAL,
        viewId,
        remoteViews,
        notification,
        notificationId,
        notificationTag);
  }

  /**
   * Constructor using a Notification object, a notificationId, and a notificationTag to get a
   * handle on the Notification in order to update it.
   *
   * @param context Context to use in the AppWidgetManager initialization.
   * @param width Desired width of the bitmap that will be loaded.(Need to be manually put because
   *     of RemoteViews limitations.)
   * @param height Desired height of the bitmap that will be loaded. (Need to be manually put
   *     because of RemoteViews limitations.)
   * @param viewId The id of the ImageView view that will load the image.
   * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
   * @param notification The Notification object that we want to update.
   * @param notificationId The notificationId of the Notification that we want to load the Bitmap.
   * @param notificationTag The notificationTag of the Notification that we want to load the Bitmap.
   *     May be {@code null}.
   */
  @SuppressLint("InlinedApi")
  // Alert users of Glide to have this permission.
  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  public NotificationTarget(
      Context context,
      int width,
      int height,
      int viewId,
      RemoteViews remoteViews,
      Notification notification,
      int notificationId,
      String notificationTag) {
    super(width, height);
    this.context = Preconditions.checkNotNull(context, "Context must not be null!");
    this.notification =
        Preconditions.checkNotNull(notification, "Notification object can not be null!");
    this.remoteViews =
        Preconditions.checkNotNull(remoteViews, "RemoteViews object can not be null!");
    this.viewId = viewId;
    this.notificationId = notificationId;
    this.notificationTag = notificationTag;
  }

  /** Updates the Notification after the Bitmap resource is loaded. */
  @SuppressLint("InlinedApi")
  // Help tools to recognize that this method requires a permission, because it posts a notification.
  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  private void update() {
    NotificationManager manager =
        (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
    Preconditions.checkNotNull(manager)
        .notify(this.notificationTag, this.notificationId, this.notification);
  }

  @SuppressLint("InlinedApi")
  // Help tools to recognize that this method requires a permission, because it calls setBitmap().
  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  @Override
  public void onResourceReady(
      @NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
    setBitmap(resource);
  }

  @SuppressLint("InlinedApi")
  // Help tools to recognize that this method requires a permission, because it calls setBitmap().
  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  @Override
  public void onLoadCleared(@Nullable Drawable placeholder) {
    setBitmap(null);
  }

  @SuppressLint("InlinedApi")
  // Help tools to recognize that this method requires a permission, because it calls update().
  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  private void setBitmap(@Nullable Bitmap bitmap) {
    this.remoteViews.setImageViewBitmap(this.viewId, bitmap);
    this.update();
  }
}
