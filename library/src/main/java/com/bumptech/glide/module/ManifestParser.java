package com.bumptech.glide.module;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses {@link com.bumptech.glide.module.GlideModule} references out of the AndroidManifest file.
 */
public final class ManifestParser {
    private static final String GLIDE_MODULE_VALUE = "GlideModule";

    private final Context context;

    public ManifestParser(Context context) {
        this.context = context;
    }

    public List<GlideModule> parse() {
        List<GlideModule> modules = new ArrayList<GlideModule>();
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                for (String key : appInfo.metaData.keySet()) {
                    if (GLIDE_MODULE_VALUE.equals(appInfo.metaData.get(key))) {
                        modules.add(parseModule(key));
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Unable to find metadata to parse GlideModules", e);
        }

        return modules;
    }

    private static GlideModule parseModule(String className) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to find GlideModule implementation", e);
        }

        Object module;
        try {
            module = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to instantiate GlideModule implementation for " + clazz, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to instantiate GlideModule implementation for " + clazz, e);
        }

        if (!(module instanceof GlideModule)) {
            throw new RuntimeException("Expected instanceof GlideModule, but found: " + module);
        }
        return (GlideModule) module;
    }
}
