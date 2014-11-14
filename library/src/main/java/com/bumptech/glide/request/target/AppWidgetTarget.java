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
 * @param <Z> Generic type of resource.
 */
public class AppWidgetTarget<Z> extends SimpleTarget<Z> {

    private int[] widgetIds;
    private ComponentName componentName;
    private final RemoteViews remoteViews;
    private final Context context;
    private final int viewId;

    /**
     * Constructor using an int array of widgetIds,
     * to get a handle on the Widget,
     * in order to update it.
     *
     * @param context     Context to use in the AppWidgetManager initialization.
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param viewId      The id of the ImageView view that will load the image.
     * @param width       Desired width of the bitmap that will be loaded.(Need to be manually set
     *                    because of RemoteViews limitations.)
     * @param height      Desired height of the bitmap that will be loaded. (Need to be manually set
     *                    because of RemoteViews limitations.)
     * @param widgetIds   The int[] that contains the widget ids of an application.
     */
    public AppWidgetTarget(Context context, RemoteViews remoteViews,
                           int viewId, int width, int height, int... widgetIds) {
        super(width, height);
        if (widgetIds == null) {
            throw new NullPointerException("WidgetIds can not be null!");
        }
        if (remoteViews == null) {
            throw new NullPointerException("RemoteViews object can not be null!");
        }
        this.context = context;
        this.remoteViews = remoteViews;
        this.viewId = viewId;
        this.widgetIds = widgetIds;
    }

    /**
     * Constructor using an int array of widgetIds, when override has been set,
     * to get a handle on the Widget,
     * in order to update it.
     *
     * @param context     Context to use in the AppWidgetManager initialization.
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param viewId      The id of the ImageView view that will load the image.
     * @param widgetIds   The int[] that contains the widget ids of an application.
     */
    public AppWidgetTarget(Context context, RemoteViews remoteViews, int viewId, int... widgetIds) {
        this(context, remoteViews, viewId, -1, -1, widgetIds);
    }

    /**
     * Constructor using a ComponentName,
     * to get a handle on the Widget,
     * in order to update it.
     *
     * @param context     Context to use in the AppWidgetManager initialization.
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param viewId      The id of the ImageView view that will load the image.
     * @param width       Desired width of the bitmap that will be loaded.(Need to be manually set
     *                    because of RemoteViews limitations.)
     * @param height      Desired height of the bitmap that will be loaded. (Need to be manually set
     *                    because of RemoteViews limitations.)
     * @param componentName   The ComponentName that refers to our AppWidget.
     */
    public AppWidgetTarget(Context context, RemoteViews remoteViews,
                           int viewId, int width, int height, ComponentName componentName) {
        super(width, height);
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
    }

    /**
     * Constructor using a ComponentName, when override has been set,
     * to get a handle on the Widget,
     * in order to update it.
     *
     * @param context     Context to use in the AppWidgetManager initialization.
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param viewId      The id of the ImageView view that will load the image.
     * @param componentName   The ComponentName that refers to our AppWidget.
     */
    public AppWidgetTarget(Context context, RemoteViews remoteViews,
                           int viewId, ComponentName componentName) {
        this(context, remoteViews, viewId, -1, -1, componentName);
    }

    /**
     * Updates the AppWidget after the ImageView has loaded the Bitmap.
     */
    public void update() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.context);
        if (this.componentName != null) {
            appWidgetManager.updateAppWidget(this.componentName, this.remoteViews);
        } else {
            appWidgetManager.updateAppWidget(this.widgetIds, this.remoteViews);
        }
    }

    @Override
    public void onResourceReady(Z resource, GlideAnimation<? super Z> glideAnimation) {
        this.remoteViews.setImageViewBitmap(this.viewId, (Bitmap) resource);
        this.update();
    }

}
