package com.bumptech.glide.manager;

import com.bumptech.glide.request.Request;

public interface RequestManager {

    public void addRequest(Request request);

    public void removeRequest(Request request);
}
