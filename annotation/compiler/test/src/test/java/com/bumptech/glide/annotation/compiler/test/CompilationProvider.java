package com.bumptech.glide.annotation.compiler.test;

import com.google.testing.compile.Compilation;

/** Provides the {@link Compilation} used to compile test code. */
public interface CompilationProvider {
  Compilation getCompilation();
}
