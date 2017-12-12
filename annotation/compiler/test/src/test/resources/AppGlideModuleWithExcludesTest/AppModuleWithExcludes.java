package com.bumptech.glide.test;

import com.bumptech.glide.annotation.Excludes;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.test.EmptyLibraryModule;

@GlideModule
@Excludes(EmptyLibraryModule.class)
public final class AppModuleWithExcludes extends AppGlideModule {}