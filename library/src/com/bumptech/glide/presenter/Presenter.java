package com.bumptech.glide.presenter;

public interface Presenter<T> {

    public void setModel(T model);

    public void resetPlaceHolder();

    public void clear();
}
