package com.bumptech.glide.request;

public interface GlideAnimationFactory<R> {

    public GlideAnimation<R> build(boolean isFromMemoryCache, boolean isFirstImage);

}
