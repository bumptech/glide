package com.bumptech.glide.manager;
/** A no-op {@link com.bumptech.glide.manager.ConnectivityStrategy}. */
public class NullConnectivityStrategyImpl implements ConnectivityStrategy {

  @Override
  public void register() {}

  @Override
  public void unregister() {}
}
