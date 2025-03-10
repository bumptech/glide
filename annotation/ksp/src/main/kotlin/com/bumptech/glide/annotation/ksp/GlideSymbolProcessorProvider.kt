package com.bumptech.glide.annotation.ksp

import com.google.devtools.kotlin.ksp.metainf.MetaInfServices
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

@MetaInfServices(implementing = [SymbolProcessorProvider::class])
class GlideSymbolProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return GlideSymbolProcessor(environment)
  }
}
