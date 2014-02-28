package com.bumptech.glide.presenter.target;

import com.bumptech.glide.presenter.ImagePresenter;

public abstract class BaseTarget implements Target {

    private ImagePresenter imagePresenter;

    @Override
    public void setImagePresenter(ImagePresenter imagePresenter) {
        this.imagePresenter = imagePresenter;
    }

    @Override
    public ImagePresenter getImagePresenter() {
        return imagePresenter;
    }
}
