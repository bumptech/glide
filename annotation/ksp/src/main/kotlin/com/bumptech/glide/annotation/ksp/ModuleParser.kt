package com.bumptech.glide.annotation.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode

object ModuleParser {

  internal data class GlideModules(
    val appModules: List<KSClassDeclaration>,
    val libraryModules: List<KSClassDeclaration>,
  )

  internal fun extractGlideModules(annotatedModules: List<KSNode>): GlideModules {
    val appAndLibraryModuleNames = listOf(APP_MODULE_QUALIFIED_NAME, LIBRARY_MODULE_QUALIFIED_NAME)
    val modulesBySuperType: Map<String?, List<KSClassDeclaration>> =
      annotatedModules.filterIsInstance<KSClassDeclaration>().groupBy { classDeclaration ->
        appAndLibraryModuleNames.firstOrNull { classDeclaration.hasSuperType(it) }
      }

    val (appModules, libraryModules) =
      appAndLibraryModuleNames.map { modulesBySuperType[it] ?: emptyList() }
    return GlideModules(appModules, libraryModules)
  }

  private fun KSClassDeclaration.hasSuperType(superTypeQualifiedName: String): Boolean {
    val superDeclarations = superTypes
            .map { superType -> superType.resolve().declaration }
    val hasInDirectParent = superDeclarations
            .map { it.qualifiedName!!.asString() }
            .contains(superTypeQualifiedName)
    return if (hasInDirectParent) {
      true
    } else {
      superDeclarations.filterIsInstance(KSClassDeclaration::class.java)
              .any { it.hasSuperType(superTypeQualifiedName) }
    }
  }

  private const val APP_MODULE_QUALIFIED_NAME = "com.bumptech.glide.module.AppGlideModule"
  private const val LIBRARY_MODULE_QUALIFIED_NAME = "com.bumptech.glide.module.LibraryGlideModule"
}