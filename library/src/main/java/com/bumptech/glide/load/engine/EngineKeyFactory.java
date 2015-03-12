package com.bumptech.glide.load.engine;

class EngineKeyFactory {

  @SuppressWarnings("rawtypes")
  public EngineKey buildKey(RequestContext<?, ?> requestContext, int width, int height) {
    return new EngineKey(requestContext.getModel(), requestContext.getSignature(), width, height,
        requestContext.getTransformations(), requestContext.getResourceClass(),
        requestContext.getTranscodeClass(), requestContext.getOptions());
  }
}
