package com.bumptech.glide.load.engine;

class EngineKeyFactory {

    @SuppressWarnings("rawtypes")
    public EngineKey buildKey(RequestContext<?, ?> requestContext, int width, int height) {
        return new EngineKey(requestContext.getId(), requestContext.getSignature(), width, height,
                requestContext.getResourceClass(), requestContext.getTranscodeClass());
    }

}
