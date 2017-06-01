package com.bumptech.glide.samples.flickr;

import android.app.Application;

/**
 * An activity that allows users to search for images on Flickr and that contains a series of
 * fragments that display retrieved image thumbnails.
 */
public class App extends Application {

    private static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override

    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
