package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.File;
import java.util.List;

public class LoadDebuggerImpl implements LoadDebugger {
    private static final String TAG = "LoadDebugger";
    private static final String INDENT = "    ";
    private final StringBuilder sb = new StringBuilder();

    @Override
    public void startDecodeResultFromCache() {
        sb.append("*** Start Decode Result From Cache ***");
        nl();
    }

    @Override
    public void endDecodeResultFromCache() {
        sb.append("*** End Decode Result From Cache ***");
        nl();
    }

    @Override
    public void appendResultCacheDecodeResult(Key key, Resource<?> resource, Exception e) {
        if (resource == null) {
            indent().append("Key: ").append(key).append(" Failed: ").append(e.getMessage());
            nl();
        } else {
            indent().append("Key: ").append(key).append(" Succeeded: ").append(resource);
            nl();
        }
    }

    @Override
    public void startDecodeSourceFromCache() {
        sb.append("*** Start Decode Source From Cache ***");
        nl();
    }

    @Override
    public void endDecodeSourceFromCache() {
        sb.append("*** End Decode Source From Cache ***");
        nl();
    }

    @Override
    public void appendFoundCacheFile(Key key, File file) {
        indent().append("Key: ").append(key).append(" found: ").append(file.getName());
        nl();
    }

    @Override
    public void appendMissingCacheFile(Key key) {
        indent().append("Key: ").append(key).append(" missing");
        nl();
    }

    @Override
    public <R> void appendStartLoadPaths(List<? extends LoadPath<?, ?, R>> paths) {
        indent().append("=== Start Load Paths ===");
        nl();
        for (LoadPath<?, ?, ?> path : paths) {
            indent(2).append("Path: ").append(path.getDebugString());
            nl();
            for (String decodePathString : path.getDecodePathDebugStrings()) {
                indent(3).append("Decode path: ").append(decodePathString);
                nl();
            }
        }
    }

    @Override
    public void appendEndLoadPaths() {
        indent().append("=== End Load Paths ===");
        nl();
    }

    @Override
    public void appendStartLoadPath(Object data, DataFetcher<?> fetcher) {
        indent(2).append("--- Start Load Path ---");
        nl();
        indent(3).append("Fetcher: ").append(fetcher).append(" data: ").append(data);
        nl();
    }

    @Override
    public void appendDecodePath(DecodePath<?, ?, ?> path, Resource<?> result) {
        indent(3).append("+++ Start Decode Path +++");
        nl();
        indent(4).append("From: ").append(path).append(" decoded: ").append(result);
        nl();
        indent(3).append("+++ End Decode Path +++");
        nl();
    }

    @Override
    public void appendEndLoadPath(Exception e) {
        indent(3).append("Failed: ").append(e.getMessage());
        nl();
        indent(2).append("=== End Load Path ===");
        nl();
    }

    @Override
    public void appendEndLoadPath(Resource<?> result) {
        indent(3).append("Loaded ").append(result);
        nl();
        indent(2).append("=== End Load Path ===");
        nl();
    }

    @Override
    public void startCacheAndDecodeSource() {
        sb.append("*** Start Cache And Decode Source ***");
        nl();
    }

    @Override
    public void endCacheAndDecodeSource() {
        sb.append("*** End Cache And Decode Source ***");
        nl();
    }

    @Override
    public void startEncodeFromFetcher(DataFetcher fetcher) {
        indent(2).append("=== Start Encode From Fetcher ===");
        nl();
        indent(2).append(" Fetcher: ").append(fetcher);
        nl();
    }

    @Override
    public void appendDataFromFetcher(Object data) {
        indent(2).append("Data: ").append(data);
        nl();
    }

    @Override
    public void appendWriteDataToCache(Key key) {
        indent(2).append("Wrote to: ").append(key);
        nl();
    }

    @Override
    public void endEncodeFromFetcher() {
        indent().append("=== End Encode From Fetcher ===");
        nl();
    }

    @Override
    public void startDecodeFromSource() {
        sb.append("*** Start Decode From Source ***");
        nl();
    }

    @Override
    public void endDecodeFromSource() {
        sb.append("*** End Decode From Source ***");
        nl();
    }

    private StringBuilder indent() {
        return indent(1);
    }

    private StringBuilder indent(int level) {
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
        return sb;
    }

    private void nl() {
        sb.append("\n");
    }

    @Override
    public void writeLogs(String tag) {
        if (Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, sb.toString());
        } else if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, sb.toString());
        }
    }
}
