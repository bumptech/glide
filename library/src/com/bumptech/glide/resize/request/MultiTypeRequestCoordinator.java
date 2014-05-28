package com.bumptech.glide.resize.request;

public class MultiTypeRequestCoordinator implements RequestCoordinator, Request {
    private final RequestCoordinator parent;
    private Request[] requests;

    public MultiTypeRequestCoordinator(RequestCoordinator coordinator) {
        this.parent = coordinator;
    }

    public void setRequests(Request... requests) {
        this.requests = requests;
    }

    @Override
    public void run() {
        for (Request request : requests) {
            request.run();
            if (request.isComplete()) {
                break;
            }
        }
    }

    @Override
    public void clear() {
        for (Request request : requests) {
            request.clear();
        }
    }

    @Override
    public boolean isComplete() {
        return isAnyRequestComplete();
    }

    @Override
    public boolean isFailed() {
        for (Request request : requests) {
            if (!request.isFailed()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canSetImage(Request request) {
        return !isAnyRequestComplete() && (parent == null || parent.canSetImage(this));
    }

    @Override
    public boolean canSetPlaceholder(Request request) {
        return ((request == requests[0] && !isAnyRequestComplete()) || isFailed())
                && (parent == null || parent.canSetPlaceholder(this));
    }

    @Override
    public boolean isAnyRequestComplete() {
        if (parent != null && parent.isAnyRequestComplete()) {
            return true;
        }
         for (Request request : requests) {
            if (request.isComplete()) {
                return true;
            }
        }
        return false;
    }
}
