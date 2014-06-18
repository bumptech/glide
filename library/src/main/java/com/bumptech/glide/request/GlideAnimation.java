package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.bumptech.glide.request.target.Target;

public interface GlideAnimation<R> {
    public boolean animate(Drawable previous, R current, View view, Target<R> target);
}
