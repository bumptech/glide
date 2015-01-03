package com.bumptech.glide.load.engine;

class EngineKeyFactory {

    @SuppressWarnings("rawtypes")
    public EngineKey buildKey(RequestContext<?, ?> requestContext, int width, int height) {
        // TODO: what if I request a bitmap for an animated GIF, cache just the Bitmap, and then ask for the animated
        // gif?
        return new EngineKey(requestContext.getId(), requestContext.getSignature(), width, height,
                requestContext.getTransformation(), requestContext.getResourceClass(),
                requestContext.getTranscodeClass());
    }

}
