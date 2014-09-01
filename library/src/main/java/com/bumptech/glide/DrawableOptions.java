package com.bumptech.glide;

import android.view.animation.Animation;

interface DrawableOptions {

    /**
     * Applies a cross fade transformation that fades from the placeholder to the loaded
     * {@link android.graphics.drawable.Drawable}. If no placeholder is set, the Drawable will instead simply fade in.
     *
     * @see #crossFade(int)
     * @see #crossFade(int, int)
     * @see #crossFade(android.view.animation.Animation, int)
     *
     * @return This request builder.
     */
     public GenericRequestBuilder<?, ?, ?, ?> crossFade();

    /**
     * Applies a cross fade transformation that fades from the placeholder to the loaded
     * {@link android.graphics.drawable.Drawable}. If no placeholder is set the Drawable will instead simply fade in.
     *
     * @see #crossFade()
     * @see #crossFade(int, int)
     * @see #crossFade(android.view.animation.Animation, int)
     *
     * @param duration The duration of the cross fade and initial fade in.
     * @return This request builder.
     */
    public GenericRequestBuilder<?, ?, ?, ?> crossFade(int duration);


    /**
     * Applies a cross fade transformation that des from the placeholder to the loaded
     * {@link android.graphics.drawable.Drawable}. If no placeholder is set, the Drawable will instead be animated in
     * using the given {@link android.view.animation.Animation}.
     *
     * @see #crossFade()
     * @see #crossFade(int)
     * @see #crossFade(int, int)
     *
     * @param animation The Animation to use if no placeholder is set.
     * @param duration The duration of the cross fade animation.
     * @return This request builder.
     */
    public GenericRequestBuilder<?, ?, ?, ?> crossFade(Animation animation, int duration);


    /**
     * Applies a cross fade transformation that des from the placeholder to the loaded
     * {@link android.graphics.drawable.Drawable}. If no placeholder is set, the Drawable will instead be animated in
     * using the {@link android.view.animation.Animation} loaded from the given animation id.
     *
     * @see #crossFade()
     * @see #crossFade(int)
     * @see #crossFade(android.view.animation.Animation, int)
     *
     * @param animationId The id of the Animation to use if no placeholder is set.
     * @param duration The duration of the cross fade animation.
     * @return This request builder.
     */
    public GenericRequestBuilder<?, ?, ?, ?> crossFade(int animationId, int duration);
}
