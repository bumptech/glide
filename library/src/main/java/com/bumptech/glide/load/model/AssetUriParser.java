package com.bumptech.glide.load.model;

import android.content.ContentResolver;
import android.net.Uri;

/**
 * A utility class for parsing Asset uris that look like: file:///android_asset/some/path/in/assets/folder.
 */
final class AssetUriParser {
    private static final String ASSET_PATH_SEGMENT = "android_asset";
    private static final String ASSET_PREFIX = ContentResolver.SCHEME_FILE + ":///" + ASSET_PATH_SEGMENT + "/";
    private static final int ASSET_PREFIX_LENGTH = ASSET_PREFIX.length();

    private AssetUriParser() {
        // Utility constructor.
    }

    /**
     * Returns true if the given {@link android.net.Uri} matches the asset uri pattern.
     */
    public static boolean isAssetUri(Uri uri) {
        return ContentResolver.SCHEME_FILE.equals(uri.getScheme()) && !uri.getPathSegments().isEmpty()
                && ASSET_PATH_SEGMENT.equals(uri.getPathSegments().get(0));
    }

    /**
     * Returns the string path for the given asset uri.
     *
     * <p>
     *     Assumes the given {@link android.net.Uri} is in fact an asset uri.
     * </p>
     */
    public static String toAssetPath(Uri uri) {
        return uri.toString().substring(ASSET_PREFIX_LENGTH);
    }
}
