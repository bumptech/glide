package com.bumptech.glide.integration.cronet;

import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import java.nio.ByteBuffer;

/** An {@link DataFetcher} for fetching {@link GlideUrl} using cronet. */
final class ChromiumUrlFetcher<T> implements DataFetcher<T>, ChromiumRequestSerializer.Listener {

  private final ChromiumRequestSerializer serializer;
  private final ByteBufferParser<T> parser;
  private final GlideUrl url;

  private DataCallback<? super T> callback;

  public ChromiumUrlFetcher(
      ChromiumRequestSerializer serializer, ByteBufferParser<T> parser, GlideUrl url) {
    this.serializer = serializer;
    this.parser = parser;
    this.url = url;
  }

  @Override
  public void loadData(Priority priority, DataCallback<? super T> callback) {
    this.callback = callback;
    serializer.startRequest(priority, url, this);
  }

  @Override
  public void cleanup() {
    // Nothing to cleanup.
  }

  @Override
  public void cancel() {
    serializer.cancelRequest(url, this);
  }

  @Override
  public Class<T> getDataClass() {
    return parser.getDataClass();
  }

  @Override
  public DataSource getDataSource() {
    return DataSource.REMOTE;
  }

  @Override
  public void onRequestComplete(ByteBuffer byteBuffer) {
    callback.onDataReady(parser.parse(byteBuffer));
  }

  @Override
  public void onRequestFailed(@Nullable Exception e) {
    callback.onLoadFailed(e);
  }
}
