package com.bumptech.glide.test;

import com.bumptech.glide.annotation.Excludes;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.test._package.LibraryModuleInPackage;

@GlideModule
@Excludes(LibraryModuleInPackage.class)
public final class AppModuleWithLibraryInPackage extends AppGlideModule {}
