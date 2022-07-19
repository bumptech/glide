package com.bumptech.glide.annotation.ksp

import com.bumptech.glide.annotation.Excludes
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import kotlin.reflect.KClass

// This class is visible only for testing
// TODO(b/174783094): Add @VisibleForTesting when internal is supported.
object AppGlideModuleConstants {
  // This variable is visible only for testing
  // TODO(b/174783094): Add @VisibleForTesting when internal is supported.
  const val INVALID_MODULE_MESSAGE =
    "Your AppGlideModule must have at least one constructor that has either no parameters or " +
      "accepts only a Context."

  private const val CONTEXT_NAME = "Context"
  internal const val CONTEXT_PACKAGE = "android.content"
  internal const val GLIDE_PACKAGE_NAME = "com.bumptech.glide"
  internal const val CONTEXT_QUALIFIED_NAME = "$CONTEXT_PACKAGE.$CONTEXT_NAME"
  internal const val GENERATED_ROOT_MODULE_PACKAGE_NAME = GLIDE_PACKAGE_NAME

  internal val CONTEXT_CLASS_NAME = ClassName(CONTEXT_PACKAGE, CONTEXT_NAME)
}

internal data class AppGlideModuleData(
  val name: ClassName,
  val constructor: Constructor,
) {
  internal data class Constructor(val hasContext: Boolean)
}

/**
 * Given a [com.bumptech.glide.module.AppGlideModule] class declaration provided by the developer,
 * validate the class and produce a fully parsed [AppGlideModuleData] that allows us to generate a
 * valid [com.bumptech.glide.GeneratedAppGlideModule] implementation without further introspection.
 */
internal class AppGlideModuleParser(
  private val environment: SymbolProcessorEnvironment,
  private val resolver: Resolver,
  private val appGlideModuleClass: KSClassDeclaration,
) {

  fun parseAppGlideModule(): AppGlideModuleData {
    val constructor = parseAppGlideModuleConstructorOrThrow()
    val name = ClassName.bestGuess(appGlideModuleClass.qualifiedName!!.asString())

    return AppGlideModuleData(name = name, constructor = constructor)
  }

  private fun parseAppGlideModuleConstructorOrThrow(): AppGlideModuleData.Constructor {
    val hasEmptyConstructors = appGlideModuleClass.getConstructors().any { it.parameters.isEmpty() }
    val hasContextParamOnlyConstructor =
      appGlideModuleClass.getConstructors().any { it.hasSingleContextParameter() }
    if (!hasEmptyConstructors && !hasContextParamOnlyConstructor) {
      throw InvalidGlideSourceException(AppGlideModuleConstants.INVALID_MODULE_MESSAGE)
    }
    return AppGlideModuleData.Constructor(hasContextParamOnlyConstructor)
  }

  private fun KSFunctionDeclaration.hasSingleContextParameter() =
    parameters.size == 1 &&
      AppGlideModuleConstants.CONTEXT_QUALIFIED_NAME ==
        parameters.single().type.resolve().declaration.qualifiedName?.asString()

  private data class IndexFilesAndLibraryModuleNames(
    val indexFiles: List<KSDeclaration>,
    val libraryModuleNames: List<String>,
  )

  private fun extractGlideModulesFromIndexAnnotation(
    index: KSDeclaration,
  ): List<String> {
    val indexAnnotation: KSAnnotation = index.atMostOneIndexAnnotation() ?: return emptyList()
    environment.logger.info("Found index annotation: $indexAnnotation")
    return indexAnnotation.getModuleArgumentValues().toList()
  }

  private fun KSAnnotation.getModuleArgumentValues(): List<String> {
    val result = arguments.find { it.name?.getShortName().equals("modules") }?.value
    if (result is List<*> && result.all { it is String }) {
      @Suppress("UNCHECKED_CAST") return result as List<String>
    }
    throw InvalidGlideSourceException("Found an invalid internal Glide index: $this")
  }

  private fun KSDeclaration.atMostOneIndexAnnotation() = atMostOneAnnotation(Index::class)

  private fun KSDeclaration.atMostOneExcludesAnnotation() = atMostOneAnnotation(Excludes::class)

  private fun KSDeclaration.atMostOneAnnotation(
    annotation: KClass<out Annotation>,
  ): KSAnnotation? {
    val matchingAnnotations: List<KSAnnotation> =
      annotations
        .filter {
          annotation.qualifiedName?.equals(
            it.annotationType.resolve().declaration.qualifiedName?.asString()
          )
            ?: false
        }
        .toList()
    if (matchingAnnotations.size > 1) {
      throw InvalidGlideSourceException(
        """Expected 0 or 1 $annotation annotations on ${this.qualifiedName}, but found: 
          ${matchingAnnotations.size}"""
      )
    }
    return matchingAnnotations.singleOrNull()
  }
}

/**
 * Given valid [AppGlideModuleData], writes a Kotlin implementation of
 * [com.bumptech.glide.GeneratedAppGlideModule].
 *
 * This class should obtain all of its data from [AppGlideModuleData] and should not interact with
 * any ksp classes. In the long run, the restriction may allow us to share code between the Java and
 * Kotlin processors.
 */
internal class AppGlideModuleGenerator(private val appGlideModuleData: AppGlideModuleData) {

  fun generateAppGlideModule(): FileSpec {
    val generatedAppGlideModuleClass = generateAppGlideModuleClass(appGlideModuleData)
    return FileSpec.builder(
        AppGlideModuleConstants.GLIDE_PACKAGE_NAME,
        "GeneratedAppGlideModuleImpl"
      )
      .addType(generatedAppGlideModuleClass)
      .build()
  }

  private fun generateAppGlideModuleClass(
    data: AppGlideModuleData,
  ): TypeSpec {
    return TypeSpec.classBuilder("GeneratedAppGlideModuleImpl")
      .superclass(
        ClassName(
          AppGlideModuleConstants.GENERATED_ROOT_MODULE_PACKAGE_NAME,
          "GeneratedAppGlideModule"
        )
      )
      .addModifiers(KModifier.INTERNAL)
      .addProperty("appGlideModule", data.name, KModifier.PRIVATE)
      .primaryConstructor(generateConstructor(data))
      .addFunction(generateRegisterComponents())
      .addFunction(generateApplyOptions())
      .addFunction(generateManifestParsingDisabled())
      .build()
  }

  private fun generateConstructor(data: AppGlideModuleData): FunSpec {
    val contextParameterBuilder =
      ParameterSpec.builder("context", AppGlideModuleConstants.CONTEXT_CLASS_NAME)
    if (!data.constructor.hasContext) {
      contextParameterBuilder.addAnnotation(
        AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
          .addMember("%S", "UNUSED_VARIABLE")
          .build()
      )
    }

    return FunSpec.constructorBuilder()
      .addParameter(contextParameterBuilder.build())
      .addStatement(
        "appGlideModule = %T(${if (data.constructor.hasContext) "context" else ""})",
        data.name
      )
      .build()

    // TODO(judds): Log the discovered modules here.
  }

  // TODO(judds): call registerComponents on LibraryGlideModules here.
  private fun generateRegisterComponents() =
    FunSpec.builder("registerComponents")
      .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
      .addParameter("context", AppGlideModuleConstants.CONTEXT_CLASS_NAME)
      .addParameter("glide", ClassName(AppGlideModuleConstants.GLIDE_PACKAGE_NAME, "Glide"))
      .addParameter("registry", ClassName(AppGlideModuleConstants.GLIDE_PACKAGE_NAME, "Registry"))
      .addStatement("appGlideModule.registerComponents(context, glide, registry)")
      .build()

  private fun generateApplyOptions() =
    FunSpec.builder("applyOptions")
      .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
      .addParameter("context", AppGlideModuleConstants.CONTEXT_CLASS_NAME)
      .addParameter(
        "builder",
        ClassName(AppGlideModuleConstants.GLIDE_PACKAGE_NAME, "GlideBuilder")
      )
      .addStatement("appGlideModule.applyOptions(context, builder)")
      .build()

  private fun generateManifestParsingDisabled() =
    FunSpec.builder("isManifestParsingEnabled")
      .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
      .returns(Boolean::class)
      .addStatement("return false")
      .build()
}
