package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.File;
import java.util.List;

public class EmptyLoadDebugger implements LoadDebugger {
    @Override
    public void startDecodeResultFromCache() { }

    @Override
    public void endDecodeResultFromCache() { }

    @Override
    public void appendResultCacheDecodeResult(Key key, Resource<?> resource, Exception e) { }

    @Override
    public void startDecodeSourceFromCache() { }

    @Override
    public void endDecodeSourceFromCache() { }

    @Override
    public void appendFoundCacheFile(Key key, File file) { }

    @Override
    public void appendMissingCacheFile(Key key) { }

    @Override
    public <R> void appendStartLoadPaths(List<? extends LoadPath<?, ?, R>> paths) { }

    @Override
    public void appendEndLoadPaths() { }

    @Override
    public void appendStartLoadPath(Object data, DataFetcher<?> fetcher) { }

    @Override
    public void appendDecodePath(DecodePath<?, ?, ?> path, Resource<?> result) { }

    @Override
    public void appendEndLoadPath(Exception e) { }

    @Override
    public void appendEndLoadPath(Resource<?> result) { }

    @Override
    public void startCacheAndDecodeSource() { }

    @Override
    public void endCacheAndDecodeSource() { }

    @Override
    public void startEncodeFromFetcher(DataFetcher fetcher) { }

    @Override
    public void appendDataFromFetcher(Object data) { }

    @Override
    public void appendWriteDataToCache(Key key) { }

    @Override
    public void endEncodeFromFetcher() { }

    @Override
    public void startDecodeFromSource() { }

    @Override
    public void endDecodeFromSource() { }

    @Override
    public void writeLogs(String tag) { }
}
