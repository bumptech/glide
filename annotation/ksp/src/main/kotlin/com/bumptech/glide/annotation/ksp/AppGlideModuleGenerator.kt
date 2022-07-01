package com.bumptech.glide.annotation.ksp

import com.bumptech.glide.annotation.Excludes
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import kotlin.reflect.KClass

object AppGlideModuleConstants {
  const val INVALID_MODULE_MESSAGE =
    "Your AppGlideModule must have at least one constructor that has either no parameters or " +
      "accepts only a Context."
}

private const val GLIDE_PACKAGE_NAME = "com.bumptech.glide"
private const val CONTEXT_PACKAGE = "android.content"
private const val CONTEXT_NAME = "Context"
private const val CONTEXT_QUALIFIED_NAME = "$CONTEXT_PACKAGE.$CONTEXT_NAME"
private const val GENERATED_ROOT_MODULE_PACKAGE_NAME = GLIDE_PACKAGE_NAME
private val CONTEXT_CLASS_NAME = ClassName(CONTEXT_PACKAGE, CONTEXT_NAME)

internal data class AppGlideModuleData(
  val name: ClassName,
  val constructor: Constructor,
  val allowedLibraryGlideModuleNames: List<String>,
  val sources: List<KSDeclaration>,
) {
  internal data class Constructor(val hasContext: Boolean)
}

internal class AppGlideModuleParser(
  private val environment: SymbolProcessorEnvironment,
  private val resolver: Resolver,
  private val appGlideModuleClass: KSClassDeclaration,
) {

  fun parseAppGlideModule(): AppGlideModuleData {
    val constructor = parseAppGlideModuleConstructorOrThrow()
    val name = ClassName.bestGuess(appGlideModuleClass.qualifiedName!!.asString())

    val (indexFiles, allLibraryModuleNames) = getIndexesAndLibraryGlideModuleNames()
    val excludedGlideModuleClassNames = getExcludedGlideModuleClassNames()
    val filteredGlideModuleClassNames =
      allLibraryModuleNames.filterNot { excludedGlideModuleClassNames.contains(it) }

    return AppGlideModuleData(
      name = name,
      constructor = constructor,
      allowedLibraryGlideModuleNames = filteredGlideModuleClassNames,
      sources = indexFiles)
  }

  private fun getExcludedGlideModuleClassNames(): List<String> {
    val excludesAnnotation = appGlideModuleClass.atMostOneExcludesAnnotation()
    // TODO(judds): Do something with this.
    environment.logger.logging(
      "Found excludes annotation arguments: ${excludesAnnotation?.arguments}"
    )
    return listOf()
  }

  private fun parseAppGlideModuleConstructorOrThrow(): AppGlideModuleData.Constructor {
    val hasEmptyConstructors =
      appGlideModuleClass.getConstructors().any { it.parameters.isEmpty() }
    val hasContextParamOnlyConstructor =
      appGlideModuleClass.getConstructors().any { it.hasSingleContextParameter() }
    if (!hasEmptyConstructors && !hasContextParamOnlyConstructor) {
      throw InvalidGlideSourceException(AppGlideModuleConstants.INVALID_MODULE_MESSAGE)
    }
    return AppGlideModuleData.Constructor(hasContextParamOnlyConstructor)
  }

  private fun KSFunctionDeclaration.hasSingleContextParameter() =
    parameters.size == 1 &&
      CONTEXT_QUALIFIED_NAME ==
      parameters.single().type.resolve().declaration.qualifiedName?.asString()

  private data class IndexFilesAndLibraryModuleNames(
    val indexFiles: List<KSDeclaration>, val libraryModuleNames: List<String>,
  )

  @OptIn(KspExperimental::class)
  private fun getIndexesAndLibraryGlideModuleNames(): IndexFilesAndLibraryModuleNames {
    val (indexFiles: MutableList<KSDeclaration>, libraryGlideModuleNames: MutableList<String>) =
      resolver.getDeclarationsFromPackage(GlideSymbolProcessorConstants.PACKAGE_NAME)
        .fold(
          Pair(mutableListOf<KSDeclaration>(), mutableListOf<String>())
        ) { pair, current ->
          val libraryGlideModuleNames = extractGlideModulesFromIndexAnnotation(current)
          if (libraryGlideModuleNames.isNotEmpty()) {
            pair.first.add(current)
            pair.second.addAll(libraryGlideModuleNames)
          }
          pair
        }

    return IndexFilesAndLibraryModuleNames(indexFiles, libraryGlideModuleNames)
  }

  private fun extractGlideModulesFromIndexAnnotation(
    index: KSDeclaration,
  ): List<String> {
    val indexAnnotation: KSAnnotation = index.atMostOneIndexAnnotation() ?: return listOf()
    environment.logger.info("Found index annotation: $indexAnnotation")
    return indexAnnotation.getModuleArgumentValues().toList()
  }

  private fun KSAnnotation.getModuleArgumentValues(): List<String> {
    val result = arguments.find { it.name?.getShortName().equals("modules") }?.value
    if (result is List<*> && result.all { it is String }) {
      @Suppress("UNCHECKED_CAST")
      return result as List<String>
    }
    throw InvalidGlideSourceException("Found an invalid internal Glide index: $this")
  }

  private fun KSDeclaration.atMostOneIndexAnnotation() =
    atMostOneAnnotation(Index::class)

  private fun KSDeclaration.atMostOneExcludesAnnotation() =
    atMostOneAnnotation(Excludes::class)

  private fun KSDeclaration.atMostOneAnnotation(
    annotation: KClass<out Annotation>,
  ): KSAnnotation? {
    val matchingAnnotations: List<KSAnnotation> =
      annotations.filter {
        annotation.qualifiedName?.equals(
          it.annotationType.resolve().declaration.qualifiedName?.asString()
        ) ?: false
      }
        .toList()
    if (matchingAnnotations.size > 1) {
      throw InvalidGlideSourceException(
        """Expected 0 or 1 $annotation annotations on the Index class, but found: 
          ${matchingAnnotations.size}"""
      )
    }
    return matchingAnnotations.singleOrNull()
  }
}

internal class AppGlideModuleGenerator(private val appGlideModuleData: AppGlideModuleData) {

  fun generateAppGlideModule(): FileSpec {
    val generatedAppGlideModuleClass =
      generateAppGlideModuleClass(appGlideModuleData)
    return FileSpec.builder(GLIDE_PACKAGE_NAME, "GeneratedAppGlideModuleImpl")
      .addType(generatedAppGlideModuleClass)
      .build()
  }

  private fun generateAppGlideModuleClass(
    data: AppGlideModuleData,
  ): TypeSpec {
    return TypeSpec.classBuilder("GeneratedAppGlideModuleImpl")
      .superclass(ClassName(GENERATED_ROOT_MODULE_PACKAGE_NAME, "GeneratedAppGlideModule"))
      .addModifiers(KModifier.INTERNAL)
      .addProperty("appGlideModule", data.name, KModifier.PRIVATE)
      .primaryConstructor(generateConstructor(data))
      .addFunction(generateRegisterComponents(data.allowedLibraryGlideModuleNames))
      .addFunction(generateApplyOptions())
      .addFunction(generateManifestParsingDisabled())
      .build()
  }

  private fun generateConstructor(data: AppGlideModuleData): FunSpec {
    return FunSpec.constructorBuilder()
      .addParameter("context", CONTEXT_CLASS_NAME)
      .addStatement(
        "appGlideModule = %T(${if (data.constructor.hasContext) "context" else ""})", data.name
      )
      .build()

    // TODO(judds): Log the discovered modules here.
  }

  private fun generateRegisterComponents(allowedGlideModuleNames: List<String>) =
    FunSpec.builder("registerComponents")
      .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
      .addParameter("context", CONTEXT_CLASS_NAME)
      .addParameter("glide", ClassName(GLIDE_PACKAGE_NAME, "Glide"))
      .addParameter("registry", ClassName(GLIDE_PACKAGE_NAME, "Registry"))
      .apply {
        allowedGlideModuleNames.forEach {
          addStatement(
            "%T().registerComponents(context, glide, registry)", ClassName.bestGuess(it)
          )
        }
      }
      .addStatement("appGlideModule.registerComponents(context, glide, registry)")
      .build()

  private fun generateApplyOptions() =
    FunSpec.builder("applyOptions")
      .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
      .addParameter("context", CONTEXT_CLASS_NAME)
      .addParameter("builder", ClassName(GLIDE_PACKAGE_NAME, "GlideBuilder"))
      .addStatement("appGlideModule.applyOptions(context, builder)")
      .build()

  private fun generateManifestParsingDisabled() =
    FunSpec.builder("isManifestParsingEnabled")
      .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
      .returns(Boolean::class)
      .addStatement("return false")
      .build()
}

