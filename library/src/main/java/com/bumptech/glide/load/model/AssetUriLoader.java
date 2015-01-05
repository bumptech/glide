package com.bumptech.glide.load.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.FileDescriptorAssetPathFetcher;
import com.bumptech.glide.load.data.StreamAssetPathFetcher;

import java.io.InputStream;

public class AssetUriLoader<Data> implements ModelLoader<Uri, Data> {
    private static final String ASSET_PATH_SEGMENT = "android_asset";
    private static final String ASSET_PREFIX = ContentResolver.SCHEME_FILE + ":///" + ASSET_PATH_SEGMENT + "/";
    private static final int ASSET_PREFIX_LENGTH = ASSET_PREFIX.length();

    private final AssetManager assetManager;
    private final AssetFetcherFactory<Data> factory;

    public AssetUriLoader(AssetManager assetManager, AssetFetcherFactory<Data> factory) {
        this.assetManager = assetManager;
        this.factory = factory;
    }

    @Override
    public DataFetcher<Data> getDataFetcher(Uri model, int width, int height) {
        String assetPath = model.toString().substring(ASSET_PREFIX_LENGTH);
        return factory.buildFetcher(assetManager, assetPath);
    }

    @Override
    public boolean handles(Uri model) {
        return ContentResolver.SCHEME_FILE.equals(model.getScheme())
                && !model.getPathSegments().isEmpty()
                && ASSET_PATH_SEGMENT.equals(model.getPathSegments().get(0));
    }

    public interface AssetFetcherFactory<Data> {
        DataFetcher<Data> buildFetcher(AssetManager assetManager, String assetPath);
    }

    public static class StreamFactory implements ModelLoaderFactory<Uri, InputStream>,
        AssetFetcherFactory<InputStream> {

        @Override
        public ModelLoader<Uri, InputStream> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new AssetUriLoader<InputStream>(context.getAssets(), this);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }

        @Override
        public DataFetcher<InputStream> buildFetcher(AssetManager assetManager, String assetPath) {
            return new StreamAssetPathFetcher(assetManager, assetPath);
        }
    }

    public static class FileDescriptorFactory implements ModelLoaderFactory<Uri, ParcelFileDescriptor>,
            AssetFetcherFactory<ParcelFileDescriptor> {

        @Override
        public ModelLoader<Uri, ParcelFileDescriptor> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new AssetUriLoader<ParcelFileDescriptor>(context.getAssets(), this);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }

        @Override
        public DataFetcher<ParcelFileDescriptor> buildFetcher(AssetManager assetManager, String assetPath) {
            return new FileDescriptorAssetPathFetcher(assetManager, assetPath);
        }
    }
}
