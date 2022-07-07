package com.bumptech.glide.annotation.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec

object GlideSymbolProcessorConstants {
  val PACKAGE_NAME: String = GlideSymbolProcessor::class.java.`package`.name
  const val SINGLE_APP_MODULE_ERROR = "You can have at most one AppGlideModule, but found: %s"
  const val DUPLICATE_LIBRARY_MODULE_ERROR =
    "LibraryGlideModules %s are included more than once, keeping only one!"
  const val INVALID_ANNOTATED_CLASS =
    "@GlideModule annotated classes must implement AppGlideModule or LibraryGlideModule: %s"
}

private const val APP_MODULE_QUALIFIED_NAME = "com.bumptech.glide.module.AppGlideModule"
private const val LIBRARY_MODULE_QUALIFIED_NAME =
  "com.bumptech.glide.module.LibraryGlideModule"

class GlideSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
  var isAppGlideModuleGenerated = false

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver.getSymbolsWithAnnotation("com.bumptech.glide.annotation.GlideModule")
    val (validSymbols, invalidSymbols) = symbols.partition{ it.validate() }.toList()
    return try {
      processChecked(resolver, symbols, validSymbols, invalidSymbols)
    } catch (e: InvalidGlideSourceException) {
      environment.logger.error(e.userMessage)
      invalidSymbols
    }
  }

  private fun processChecked(
    resolver: Resolver,
    symbols: Sequence<KSAnnotated>,
    validSymbols: List<KSAnnotated>,
    invalidSymbols: List<KSAnnotated>,
  ): List<KSAnnotated> {
    environment.logger.logging("Found symbols, valid: $validSymbols, invalid: $invalidSymbols")

    val (appGlideModules, libraryGlideModules) = extractGlideModules(validSymbols)

    if (libraryGlideModules.size + appGlideModules.size != validSymbols.count()) {
      val invalidModules =
        symbols.filter { !libraryGlideModules.contains(it) && !appGlideModules.contains(it) }
          .map { it.location.toString() }
          .toList()

      throw InvalidGlideSourceException(
        GlideSymbolProcessorConstants.INVALID_ANNOTATED_CLASS.format(invalidModules)
      )
    }

    if (appGlideModules.size > 1) {
      throw InvalidGlideSourceException(
        GlideSymbolProcessorConstants.SINGLE_APP_MODULE_ERROR.format(appGlideModules)
      )
    }

    environment.logger.logging(
      "Found AppGlideModules: $appGlideModules, LibraryGlideModules: $libraryGlideModules"
    )

    if (libraryGlideModules.isNotEmpty()) {
      if (isAppGlideModuleGenerated) {
        throw InvalidGlideSourceException(
          """Found $libraryGlideModules LibraryGlideModules after processing the AppGlideModule.
            If you generated these LibraryGlideModules via another annotation processing, either
            don't or also generate the AppGlideModule and do so in the same round as the 
            LibraryGlideModules or in a subsequent round""")
      }
      parseLibraryModulesAndWriteIndex(libraryGlideModules)
      return invalidSymbols + appGlideModules
    }

    if (appGlideModules.isNotEmpty()) {
      parseAppGlideModuleAndIndexesAndWriteGeneratedAppGlideModule(
        resolver, appGlideModules.single()
      )
    }

    return invalidSymbols
  }

  private fun parseAppGlideModuleAndIndexesAndWriteGeneratedAppGlideModule(
    resolver: Resolver, appGlideModule: KSClassDeclaration,
  ) {
    val appGlideModuleData =
      AppGlideModuleParser(environment, resolver, appGlideModule)
        .parseAppGlideModule()
    val appGlideModuleGenerator = AppGlideModuleGenerator(appGlideModuleData)
    val appGlideModuleFileSpec: FileSpec = appGlideModuleGenerator.generateAppGlideModule()
    writeFile(
      appGlideModuleFileSpec,
      appGlideModuleData.sources.mapNotNull { it.containingFile },
    )
  }

  private fun parseLibraryModulesAndWriteIndex(
    libraryGlideModuleClassDeclarations: List<KSClassDeclaration>,
  ) {
    val libraryGlideModulesParser =
      LibraryGlideModulesParser(environment, libraryGlideModuleClassDeclarations)
    val uniqueLibraryGlideModules = libraryGlideModulesParser.parseUnique()
    val index: FileSpec = IndexGenerator.generate(uniqueLibraryGlideModules.map { it.name })
    writeFile(index, uniqueLibraryGlideModules.mapNotNull { it.containingFile })
  }

  private fun writeFile(file: FileSpec, sources: List<KSFile>) {
    environment.codeGenerator.createNewFile(
      Dependencies(
        aggregating = false,
        sources = sources.toTypedArray(),
      ),
      file.packageName,
      file.name
    )
      .writer()
      .use { file.writeTo(it) }

    environment.logger.logging("Wrote file: $file")
  }

  internal data class GlideModules(
    val appModules: List<KSClassDeclaration>, val libraryModules: List<KSClassDeclaration>,
  )

  private fun extractGlideModules(annotatedModules: List<KSAnnotated>): GlideModules {
    val appAndLibraryModuleNames =
      listOf(APP_MODULE_QUALIFIED_NAME, LIBRARY_MODULE_QUALIFIED_NAME)
    val modulesBySuperType: Map<String?, List<KSClassDeclaration>> =
      annotatedModules
        .filterIsInstance<KSClassDeclaration>()
        .groupBy { classDeclaration ->
          appAndLibraryModuleNames.singleOrNull { classDeclaration.hasSuperType(it) }
        }

    val (appModules, libraryModules) =
      appAndLibraryModuleNames.map { modulesBySuperType[it] ?: emptyList() }
    return GlideModules(appModules, libraryModules)
  }

  private fun KSClassDeclaration.hasSuperType(superTypeQualifiedName: String) =
    superTypes
      .map { superType -> superType.resolve().declaration.qualifiedName!!.asString() }
      .contains(superTypeQualifiedName)
}

class InvalidGlideSourceException(val userMessage: String) : Exception(userMessage)