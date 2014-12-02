package com.bumptech.glide.request.target;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.bumptech.glide.request.animation.GlideAnimation;


/**
 * This class is used in order to display downloaded Bitmap inside an ImageView
 * of an AppWidget through RemoteViews.
 *
 * <p>
 *     Note - For cancellation to work correctly, you must pass in the same instance of this class for every subsequent
 *     load.
 * </p>
 */
public class AppWidgetTarget extends SimpleTarget<Bitmap> {

    private final int[] widgetIds;
    private final ComponentName componentName;
    private final RemoteViews remoteViews;
    private final Context context;
    private final int viewId;

    /**
     * Constructor using an int array of widgetIds to get a handle on the Widget in order to update it.
     *
     * @param context     Context to use in the AppWidgetManager initialization.
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param viewId      The id of the ImageView view that will load the image.
     * @param width       Desired width in pixels of the bitmap that will be loaded. (Needs to be manually set
     *                    because of RemoteViews limitations.)
     * @param height      Desired height in pixels of the bitmap that will be loaded. (Needs to be manually set
     *                    because of RemoteViews limitations.)
     * @param widgetIds   The int[] that contains the widget ids of an application.
     */
    public AppWidgetTarget(Context context, RemoteViews remoteViews, int viewId, int width, int height,
            int... widgetIds) {
        super(width, height);
        if (context == null) {
            throw new NullPointerException("Context can not be null!");
        }
        if (widgetIds == null) {
            throw new NullPointerException("WidgetIds can not be null!");
        }
        if (widgetIds.length == 0) {
            throw new IllegalArgumentException("WidgetIds must have length > 0");
        }
        if (remoteViews == null) {
            throw new NullPointerException("RemoteViews object can not be null!");
        }
        this.context = context;
        this.remoteViews = remoteViews;
        this.viewId = viewId;
        this.widgetIds = widgetIds;
        componentName = null;
    }

    /**
     * Constructor using an int array of widgetIds to get a handle on the Widget in order to update it that uses
     * {@link #SIZE_ORIGINAL} as the target width and height.
     *
     * @param context     Context to use in the AppWidgetManager initialization.
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param viewId      The id of the ImageView view that will load the image.
     * @param widgetIds   The int[] that contains the widget ids of an application.
     */
    public AppWidgetTarget(Context context, RemoteViews remoteViews, int viewId, int... widgetIds) {
        this(context, remoteViews, viewId, SIZE_ORIGINAL, SIZE_ORIGINAL, widgetIds);
    }

    /**
     * Constructor using a ComponentName to get a handle on the Widget in order to update it.
     *
     * @param context     Context to use in the AppWidgetManager initialization.
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param viewId      The id of the ImageView view that will load the image.
     * @param width       Desired width in pixels of the bitmap that will be loaded. (Needs to be manually set
     *                    because of RemoteViews limitations.)
     * @param height      Desired height in pixels of the bitmap that will be loaded. (Needs to be manually set
     *                    because of RemoteViews limitations.)
     * @param componentName   The ComponentName that refers to our AppWidget.
     */
    public AppWidgetTarget(Context context, RemoteViews remoteViews, int viewId, int width, int height,
            ComponentName componentName) {
        super(width, height);
        if (context == null) {
            throw new NullPointerException("Context can not be null!");
        }
        if (componentName == null) {
            throw new NullPointerException("ComponentName can not be null!");
        }
        if (remoteViews == null) {
            throw new NullPointerException("RemoteViews object can not be null!");
        }
        this.context = context;
        this.remoteViews = remoteViews;
        this.viewId = viewId;
        this.componentName = componentName;
        widgetIds = null;
    }

    /**
     * Constructor using a ComponentName, when override has been set to get a handle on the Widget in order to update
     * it that uses {@link #SIZE_ORIGINAL} as the target width and height.
     *
     * @param context     Context to use in the AppWidgetManager initialization.
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param viewId      The id of the ImageView view that will load the image.
     * @param componentName   The ComponentName that refers to our AppWidget.
     */
    public AppWidgetTarget(Context context, RemoteViews remoteViews, int viewId, ComponentName componentName) {
        this(context, remoteViews, viewId, SIZE_ORIGINAL, SIZE_ORIGINAL, componentName);
    }

    /**
     * Updates the AppWidget after the ImageView has loaded the Bitmap.
     */
    private void update() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.context);
        if (this.componentName != null) {
            appWidgetManager.updateAppWidget(this.componentName, this.remoteViews);
        } else {
            appWidgetManager.updateAppWidget(this.widgetIds, this.remoteViews);
        }
    }

    @Override
    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
        this.remoteViews.setImageViewBitmap(this.viewId, resource);
        this.update();
    }
}
