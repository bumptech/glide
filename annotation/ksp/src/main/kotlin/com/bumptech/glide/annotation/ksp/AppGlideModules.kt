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
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
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
  // This variable is visible only for testing
  // TODO(b/174783094): Add @VisibleForTesting when internal is supported.
  const val INVALID_EXCLUDES_ANNOTATION_MESSAGE =
    """
     @Excludes on %s is invalid. The value argument of your @Excludes annotation must be set to 
     either a single LibraryGlideModule class or a non-empty list of LibraryGlideModule classes. 
     Remove the annotation if you do not wish to exclude any LibraryGlideModules. Include each 
     LibraryGlideModule you do wish to exclude exactly once. Do not put types other than 
     LibraryGlideModules in the argument list"""

  private const val CONTEXT_NAME = "Context"
  private const val CONTEXT_PACKAGE = "android.content"
  internal const val GLIDE_PACKAGE_NAME = "com.bumptech.glide"
  internal const val CONTEXT_QUALIFIED_NAME = "$CONTEXT_PACKAGE.$CONTEXT_NAME"
  internal const val GENERATED_ROOT_MODULE_PACKAGE_NAME = GLIDE_PACKAGE_NAME

  internal val CONTEXT_CLASS_NAME = ClassName(CONTEXT_PACKAGE, CONTEXT_NAME)
}

internal data class AppGlideModuleData(
  val name: ClassName,
  val constructor: Constructor,
  val allowedLibraryGlideModuleNames: List<String>,
  val sources: List<KSDeclaration>,
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

    val (indexFiles, allLibraryModuleNames) = getIndexesAndLibraryGlideModuleNames()
    val excludedGlideModuleClassNames = getExcludedGlideModuleClassNames()
    val filteredGlideModuleClassNames =
      allLibraryModuleNames.filterNot { excludedGlideModuleClassNames.contains(it) }

    return AppGlideModuleData(
      name = name,
      constructor = constructor,
      allowedLibraryGlideModuleNames = filteredGlideModuleClassNames,
      sources = indexFiles
    )
  }

  private fun getExcludedGlideModuleClassNames(): Set<String> {
    val excludesAnnotation = appGlideModuleClass.atMostOneExcludesAnnotation() ?: return emptySet()
    environment.logger.logging(
      "Found excludes annotation arguments: ${excludesAnnotation.arguments}"
    )
    return parseExcludesAnnotationArgumentsOrNull(excludesAnnotation)
      ?: throw InvalidGlideSourceException(
        AppGlideModuleConstants.INVALID_EXCLUDES_ANNOTATION_MESSAGE.format(
          appGlideModuleClass.qualifiedName?.asString()
        )
      )
  }

  /**
   * Given a list of arguments from an [com.bumptech.glide.annotation.Excludes] annotation, parses
   * and returns a list of qualified names of the excluded
   * [com.bumptech.glide.module.LibraryGlideModule] implementations, or returns null if the
   * arguments are invalid.
   *
   * Ideally we'd throw more specific exceptions based on the type of failure. However, there are a
   * bunch of individual failure types and they differ depending on whether the source was written
   * in Java or Kotlin. Rather than trying to describe every failure in detail, we'll just return
   * null and allow callers to describe the correct behavior.
   */
  private fun parseExcludesAnnotationArgumentsOrNull(
    excludesAnnotation: KSAnnotation
  ): Set<String>? {
    val valueArguments: List<KSType>? = excludesAnnotation.valueArgumentList()
    if (valueArguments == null || valueArguments.isEmpty()) {
      return null
    }
    if (valueArguments.any { !it.extendsLibraryGlideModule() }) {
      return null
    }
    val libraryGlideModuleNames =
      valueArguments.mapNotNull { it.declaration.qualifiedName?.asString() }
    if (libraryGlideModuleNames.size != valueArguments.size) {
      return null
    }
    val uniqueLibraryGlideModuleNames = libraryGlideModuleNames.toSet()
    if (uniqueLibraryGlideModuleNames.size != valueArguments.size) {
      return null
    }
    return uniqueLibraryGlideModuleNames
  }

  private fun KSType.extendsLibraryGlideModule(): Boolean =
    ModuleParser.extractGlideModules(listOf<KSNode>(declaration)).libraryModules.size == 1

  /**
   * Parses the `value` argument as a list of the given type, or returns `null` if the annotation
   * has multiple arguments or `value` has any entries that are not of the expected type `T`.
   *
   * `value` is the name of the default annotation parameter allowed by syntax like
   * `@Excludes(argument)` or `@Excludes(argument1, argument2)` or `@Excludes({argument1,
   * argument2})`, depending on the source type (Kotlin or Java). This method requires that the
   * annotation has exactly one `value` argument of a given type and standardizes the differences
   * KSP produces between Kotlin and Java source.
   *
   * To make this function more general purpose, we should assert that the values are of type T
   * rather just returning null. For our current single use case, returning null matches the use
   * case for the caller better than throwing.
   */
  private inline fun <reified T> KSAnnotation.valueArgumentList(): List<T>? {
    // Require that the annotation has a single value argument that points either to a single thing
    // or a list of things (A or [A, B, C]). First validate that there's exactly one parameter and
    // that it has the expected name.
    // e.g. @Excludes(value = (A or [A, B, C])) -> (A or [A, B, C])
    val valueParameterValue: Any? =
      arguments.singleOrNull().takeIf { it?.name?.asString() == "value" }?.value

    // Next unify the types by verifying that it either has a single value of T, or a List of
    // T and converting both to List<T>
    // (A or [A, B, C]) -> ([A] or [A, B, C]) with the correct types
    return when (valueParameterValue) {
      is List<*> -> valueParameterValue.asListGivenTypeOfOrNull()
      is T -> listOf(valueParameterValue)
      else -> null
    }
  }

  private inline fun <reified T> List<*>.asListGivenTypeOfOrNull(): List<T>? =
    filterIsInstance(T::class.java).takeIf { it.size == size }

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

  @OptIn(KspExperimental::class)
  private fun getIndexesAndLibraryGlideModuleNames(): IndexFilesAndLibraryModuleNames {
    val allIndexFiles: MutableList<KSDeclaration> = mutableListOf()
    val allLibraryGlideModuleNames: MutableList<String> = mutableListOf()

    val allIndexesAndLibraryModules =
      getAllLibraryNamesFromJavaIndexes() + getAllLibraryNamesFromKspIndexes()
    for ((index, libraryGlideModuleNames) in allIndexesAndLibraryModules) {
      allIndexFiles.add(index)
      allLibraryGlideModuleNames.addAll(libraryGlideModuleNames)
    }

    return IndexFilesAndLibraryModuleNames(allIndexFiles, allLibraryGlideModuleNames)
  }

  internal data class IndexAndLibraryModuleNames(
    val index: KSDeclaration,
    val libraryModuleNames: List<String>
  )

  private fun getAllLibraryNamesFromKspIndexes(): List<IndexAndLibraryModuleNames> =
    getAllLibraryNamesFromIndexes(GlideSymbolProcessorConstants.PACKAGE_NAME) { index ->
      extractGlideModulesFromKspIndexAnnotation(index)
    }

  private fun getAllLibraryNamesFromJavaIndexes(): List<IndexAndLibraryModuleNames> =
    getAllLibraryNamesFromIndexes(GlideSymbolProcessorConstants.JAVA_ANNOTATION_PACKAGE_NAME) {
      index ->
      extractGlideModulesFromJavaIndexAnnotation(index)
    }

  @OptIn(KspExperimental::class)
  private fun getAllLibraryNamesFromIndexes(
    packageName: String,
    extractLibraryModuleNamesFromIndex: (KSDeclaration) -> List<String>
  ) = buildList {
    resolver.getDeclarationsFromPackage(packageName).forEach { index: KSDeclaration ->
      val libraryGlideModuleNames = extractLibraryModuleNamesFromIndex(index)
      if (libraryGlideModuleNames.isNotEmpty()) {
        environment.logger.info(
          "Found index annotation: $index with modules: $libraryGlideModuleNames"
        )
        add(IndexAndLibraryModuleNames(index, libraryGlideModuleNames))
      }
    }
  }

  private fun extractGlideModulesFromJavaIndexAnnotation(
    index: KSDeclaration,
  ): List<String> {
    val indexAnnotation: KSAnnotation = index.atMostOneJavaIndexAnnotation() ?: return emptyList()
    return indexAnnotation.getModuleArgumentValues().toList()
  }

  private fun extractGlideModulesFromKspIndexAnnotation(
    index: KSDeclaration,
  ): List<String> {
    val indexAnnotation: KSAnnotation = index.atMostOneKspIndexAnnotation() ?: return emptyList()
    return indexAnnotation.getModuleArgumentValues().toList()
  }

  private fun KSAnnotation.getModuleArgumentValues(): List<String> {
    val result =
      arguments.find { it.name?.getShortName().equals(IndexGenerator.INDEX_MODULES_NAME) }?.value
    if (result is List<*> && result.all { it is String }) {
      @Suppress("UNCHECKED_CAST") return result as List<String>
    }
    throw InvalidGlideSourceException("Found an invalid internal Glide index: $this")
  }

  private fun KSDeclaration.atMostOneJavaIndexAnnotation() =
    atMostOneAnnotation("com.bumptech.glide.annotation.compiler.Index")
  private fun KSDeclaration.atMostOneKspIndexAnnotation() = atMostOneAnnotation(Index::class)

  private fun KSDeclaration.atMostOneExcludesAnnotation() = atMostOneAnnotation(Excludes::class)

  private fun KSDeclaration.atMostOneAnnotation(
    annotation: KClass<out Annotation>,
  ): KSAnnotation? = atMostOneAnnotation(annotation.qualifiedName)

  private fun KSDeclaration.atMostOneAnnotation(
    annotationQualifiedName: String?,
  ): KSAnnotation? {
    val matchingAnnotations: List<KSAnnotation> =
      annotations
        .filter {
          annotationQualifiedName?.equals(
            it.annotationType.resolve().declaration.qualifiedName?.asString()
          )
            ?: false
        }
        .toList()
    if (matchingAnnotations.size > 1) {
      throw InvalidGlideSourceException(
        """Expected 0 or 1 $annotationQualifiedName annotations on $qualifiedName, but found: 
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
      .addFunction(generateRegisterComponents(data.allowedLibraryGlideModuleNames))
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
          .addMember("%S", "UNUSED_PARAMETER")
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

  private fun generateRegisterComponents(allowedGlideModuleNames: List<String>) =
    FunSpec.builder("registerComponents")
      .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
      .addParameter("context", AppGlideModuleConstants.CONTEXT_CLASS_NAME)
      .addParameter("glide", ClassName(AppGlideModuleConstants.GLIDE_PACKAGE_NAME, "Glide"))
      .addParameter("registry", ClassName(AppGlideModuleConstants.GLIDE_PACKAGE_NAME, "Registry"))
      .apply {
        allowedGlideModuleNames.forEach {
          addStatement("%T().registerComponents(context, glide, registry)", ClassName.bestGuess(it))
        }
      }
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
