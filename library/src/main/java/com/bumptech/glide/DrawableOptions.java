package com.bumptech.glide;

import android.view.animation.Animation;

interface DrawableOptions {

    /**
     * Applies a cross fade transformation that fades from the placeholder to the loaded
     * {@link android.graphics.drawable.Drawable}. If no placeholder is set, the Drawable will instead simply fade in.
     *
     * @see #crossFade(int)
     * @see #crossFade(int, int)
     *
     * @return This request builder.
     */
     GenericRequestBuilder<?, ?, ?, ?> crossFade();

    /**
     * Applies a cross fade transformation that fades from the placeholder to the loaded
     * {@link android.graphics.drawable.Drawable}. If no placeholder is set the Drawable will instead simply fade in.
     *
     * @see #crossFade()
     * @see #crossFade(int, int)
     *
     * @param duration The duration of the cross fade and initial fade in.
     * @return This request builder.
     */
    GenericRequestBuilder<?, ?, ?, ?> crossFade(int duration);


    /**
     * Applies a cross fade transformation that des from the placeholder to the loaded
     * {@link android.graphics.drawable.Drawable}. If no placeholder is set, the Drawable will instead be animated in
     * using the given {@link android.view.animation.Animation}.
     *
     * @see #crossFade()
     * @see #crossFade(int)
     * @see #crossFade(int, int)
     *
     * @deprecated If this builder is used for multiple loads, using this method will result in multiple view's being
     * asked to start an animation using a single {@link android.view.animation.Animation} object which results in
     * views animating repeatedly. Use {@link #crossFade(int, int)}} instead, or be sure to call this method once
     * per call to {@link com.bumptech.glide.GenericRequestBuilder#load(Object)} to avoid re-using animation objects.
     * Scheduled to be removed in Glide 4.0.
     * @param animation The Animation to use if no placeholder is set.
     * @param duration The duration of the cross fade animation.
     * @return This request builder.
     */
    @Deprecated
    GenericRequestBuilder<?, ?, ?, ?> crossFade(Animation animation, int duration);

    /**
     * Applies a cross fade transformation that des from the placeholder to the loaded
     * {@link android.graphics.drawable.Drawable}. If no placeholder is set, the Drawable will instead be animated in
     * using the {@link android.view.animation.Animation} loaded from the given animation id.
     *
     * @see #crossFade()
     * @see #crossFade(int)
     *
     * @param animationId The id of the Animation to use if no placeholder is set.
     * @param duration The duration of the cross fade animation.
     * @return This request builder.
     */
    GenericRequestBuilder<?, ?, ?, ?> crossFade(int animationId, int duration);
}
