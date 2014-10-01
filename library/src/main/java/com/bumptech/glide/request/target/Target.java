package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;

import com.bumptech.glide.manager.LifecycleListener;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.animation.GlideAnimation;

/**
 * An interface that Glide can load a resource into and notify of relevant lifecycle events during a load.
 *
 * <p>
 *     The lifecycle events in this class are as follows:
 *     <ul>
 *         <li>onLoadStarted</li>
 *         <li>onResourceReady</li>
 *         <li>onLoadCleared</li>
 *         <li>onLoadFailed</li>
 *     </ul>
 *
 *     The typical lifecycle is onLoadStarted -> onResourceReady or onLoadFailed -> onLoadCleared. However, there are no
 *     guarantees. onLoadStarted may not be called if the resource is in memory or if the load will fail because of a
 *     null model object. onLoadCleared similarly may never be called if the target is never cleared. See the docs for
 *     the individual methods for details.
 * </p>
 *
 * @param <R> The type of resource the target can display.
 */
public interface Target<R> extends LifecycleListener {

    /**
     * A lifecycle callback that is called when a load is started.
     *
     * <p>
     *     Note - This may not be called for every load, it is possible for example for loads to fail before the load
     *     starts (when the model object is null).
     * </p>
     *
     * <p>
     *     Note - This method may be called multiple times before any other lifecycle method is called. Loads can be
     *     paused and restarted due to lifecycle or connectivity events and each restart may cause a call here.
     * </p>
     *
     * @param placeholder The placeholder drawable to optionally show, or null.
     */
    void onLoadStarted(Drawable placeholder);

    /**
     * A lifecycle callback that is called when a load fails.
     *
     * <p>
     *     Note - This may be called before {@link #onLoadStarted(android.graphics.drawable.Drawable)} if the model
     *     object is null.
     * </p>
     *
     * @param e The exception causing the load to fail, or null if no exception occurred (usually because a decoder
     *          simply returned null).
     * @param errorDrawable The error drawable to optionally show, or null.
     */
    void onLoadFailed(Exception e, Drawable errorDrawable);

    /**
     * The method that will be called when the resource load has finished.
     *
     * @param resource the loaded resource.
     */
    void onResourceReady(R resource, GlideAnimation<? super R> glideAnimation);

    /**
     * A lifecycle callback that is called when a load is cancelled and its resources are freed.
     *
     * @param placeholder The placeholder drawable to optionally show, or null.
     */
    void onLoadCleared(Drawable placeholder);

    /**
     * A method to retrieve the size of this target.
     *
     * @param cb The callback that must be called when the size of the target has been determined
     */
    void getSize(SizeReadyCallback cb);

    /**
     * Sets the current request for this target to retain, should not be called outside of Glide.
     */
    void setRequest(Request request);

    /**
     * Retrieves the current request for this target, should not be called outside of Glide.
     */
    Request getRequest();
}
