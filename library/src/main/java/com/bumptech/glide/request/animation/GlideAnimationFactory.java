package com.bumptech.glide.request.animation;

public interface GlideAnimationFactory<R> {

    public GlideAnimation<R> build(boolean isFromMemoryCache, boolean isFirstImage);

}
