package com.bumptech.glide.request.target;


import android.app.Notification;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.bumptech.glide.request.animation.GlideAnimation;

public abstract class NotificationTarget<Z> extends BaseTarget<Z> {

    private final int width, height;
    private final RemoteViews remoteViews;
    private final Context context;
    private final int notificationId;
    private final Notification notification;
    private final int viewId;


    /**
     * Constructor using a Notification object and a notificationId,
     * to get a handle on the Notification,
     * in order to update it.
     *
     * @param context     Context to use in the AppWidgetManager initialization.
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param viewId      The id of the ImageView view that will load the image.
     * @param width       Desired width of the bitmap that will be loaded.(Need to be manually set because of RemoteViews limitations.)
     * @param height      Desired height of the bitmap that will be loaded. (Need to be manually set because of RemoteViews limitations.)
     * @param notification The Notification object that we want to update.
     * @param notificationId The notificationId of the Notification that we want to load the Bitmap.
     */
    public NotificationTarget(Context context,
                              RemoteViews remoteViews,
                              int viewId,
                              int width,
                              int height,
                              Notification notification,
                              int notificationId){
        if(notification == null){
            throw new NullPointerException("Notification object can not be null!");
        }
        if(remoteViews == null){
            throw new NullPointerException("RemoteViews object can not be null!");
        }
        this.context = context;
        this.viewId = viewId;
        this.width = width;
        this.height = height;
        this.notification = notification;
        this.notificationId = notificationId;
        this.remoteViews = remoteViews;
    }

    /**
     * Updates the Notification after the Bitmap resource is loaded.
     */
    private void update() {
        NotificationManager manager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(this.notificationId,this.notification);
    }

    @Override
    public void onResourceReady(Z resource, GlideAnimation<? super Z> glideAnimation) {
        this.remoteViews.setImageViewBitmap(this.viewId, (Bitmap) resource);
        this.update();
    }

    @Override
    public void getSize(SizeReadyCallback cb) {
        cb.onSizeReady(this.width,this.height);
    }
}
