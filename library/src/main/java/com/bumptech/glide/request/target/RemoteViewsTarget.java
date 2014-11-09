package com.bumptech.glide.request.target;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.bumptech.glide.request.animation.GlideAnimation;

public abstract class RemoteViewsTarget<Z> extends BaseTarget<Z> {

    private int mViewId;
    private int mWidth, mHeight;
    private int[] mWidgetIds;
    private final RemoteViews mRemoteViews;
    private AppWidgetManager mAppWidgetManager;
    private Context mContext;
    private ComponentName mComponentName = null;

    /**
     *
     * Constructor using an int array of widgetIds,
     * to get a handle on the Widget,
     * in order to update it.
     *
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param context Context to use in the AppWidgetManager initialization.
     * @param viewId The id of the ImageView view that will load the image.
     * @param width Desired width of the bitmap that will be loaded.(Need to be manually set because of RemoteViews limitations.)
     * @param height Desired height of the bitmap that will be loaded. (Need to be manually set because of RemoteViews limitations.)
     * @param widgetIds The int[] that contains the widget ids of an application.
     * */
    public RemoteViewsTarget(@NonNull RemoteViews remoteViews,
                             @NonNull Context context,
                             @IdRes int viewId,
                             int width,
                             int height,
                             int[] widgetIds) {
        this.mRemoteViews = remoteViews;
        this.mContext = context;
        this.mViewId = viewId;
        this.mWidth = width;
        this.mHeight = height;
        this.mWidgetIds = widgetIds;
        this.mAppWidgetManager = AppWidgetManager.getInstance(this.mContext);
    }


    /**
     * Constructor using a ComponentName object,
     * to get a handle on the Widget,
     * in order to update it.
     *
     *
     * @param remoteViews RemoteViews object which contains the ImageView that will load the bitmap.
     * @param context Context to use in the AppWidgetManager initialization.
     * @param viewId The id of the ImageView view that will load the image.
     * @param width Desired width of the bitmap that will be loaded.(Need to be manually set because of RemoteViews limitations.)
     * @param height Desired height of the bitmap that will be loaded. (Need to be manually set because of RemoteViews limitations.)
     * @param componentName The ComponentName object that provides the handle to the Widget class.
     * */
    public RemoteViewsTarget(@NonNull RemoteViews remoteViews,
                             @NonNull Context context,
                             @IdRes int viewId,
                             int width,
                             int height,
                             @NonNull ComponentName componentName) {
        this.mRemoteViews = remoteViews;
        this.mContext = context;
        this.mViewId = viewId;
        this.mWidth = width;
        this.mHeight = height;
        this.mComponentName = componentName;
        this.mAppWidgetManager = AppWidgetManager.getInstance(this.mContext);
    }

    /**
     * Updates the Widget given a RemoteViews object.
     *
     * @param remoteViews The RemoteViews object of the Widget that we want to update.
     * */
    private void update(RemoteViews remoteViews) {
        if(this.mComponentName != null){
            mAppWidgetManager.updateAppWidget(this.mComponentName, remoteViews);
        }else{
            mAppWidgetManager.updateAppWidget(this.mWidgetIds, remoteViews);
        }
    }

    @Override
    public void getSize(SizeReadyCallback cb) {
        cb.onSizeReady(this.mWidth,this.mHeight);
    }

    @Override
    public void onResourceReady(Z resource, GlideAnimation<? super Z> glideAnimation) {
        this.mRemoteViews.setImageViewBitmap(this.mViewId, (Bitmap) resource);
        this.update(this.mRemoteViews);
    }

}
