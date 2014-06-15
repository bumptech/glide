package com.bumptech.glide.manager;

import com.bumptech.glide.request.Request;

import java.util.HashSet;
import java.util.Set;

public class LifecycleRequestManager implements RequestManager {
    private final Set<Request> requests = new HashSet<Request>();

    @Override
    public void addRequest(Request request) {
        requests.add(request);
    }

    @Override
    public void removeRequest(Request request) {
        requests.remove(request);
    }

    public void onStart() {
        for (Request request : requests) {
            if (!request.isComplete() && !request.isRunning()) {
                request.run();
            }
        }

    }

    public void onStop() {
        for (Request request : requests) {
            if (!request.isComplete() && !request.isFailed()) {
                request.clear();
            }
        }
    }

    public void onDestroy() {
        for (Request request : requests) {
            request.clear();
        }
    }
}
