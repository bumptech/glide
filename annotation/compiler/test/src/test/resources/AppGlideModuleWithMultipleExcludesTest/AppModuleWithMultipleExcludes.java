package com.bumptech.glide.test;

import com.bumptech.glide.annotation.Excludes;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

@GlideModule
@Excludes({EmptyLibraryModule1.class, EmptyLibraryModule2.class})
public final class AppModuleWithMultipleExcludes extends AppGlideModule {}