package com.bumptech.glide.annotation.ksp.test

import com.bumptech.glide.annotation.ksp.GlideSymbolProcessorProvider
import com.google.common.truth.StringSubject
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import java.io.File
import java.io.FileNotFoundException
import org.intellij.lang.annotations.Language

class CompilationResult(
  private val compilation: KotlinCompilation,
  result: KotlinCompilation.Result,
) {
  val exitCode = result.exitCode
  val messages = result.messages

  fun generatedAppGlideModuleContents() = readFile(findAppGlideModule())

  fun allGeneratedFiles(): List<File> {
    val allFiles = mutableListOf<File>()
    val parentDir = generatedFilesParentDir()
    if (parentDir != null) {
      findAllFilesRecursive(parentDir, allFiles)
    }
    return allFiles
  }

  private fun findAllFilesRecursive(parent: File, allFiles: MutableList<File>) {
    if (parent.isFile) {
      allFiles.add(parent)
      return
    }
    parent.listFiles()?.map { findAllFilesRecursive(it, allFiles) }
  }

  private fun generatedFilesParentDir(): File? {
    var currentDir: File? = compilation.kspSourcesDir
    listOf("kotlin", "com", "bumptech", "glide").forEach { directoryName ->
      currentDir = currentDir?.listFiles()?.find { it.name.equals(directoryName) }
    }
    return currentDir
  }

  private fun readFile(file: File) = file.readLines().joinToString("\n")

  private fun findAppGlideModule(): File {
    return generatedFilesParentDir()?.listFiles()?.find {
      it.name.equals("GeneratedAppGlideModuleImpl.kt")
    }
      ?: throw FileNotFoundException(
        "GeneratedAppGlideModuleImpl.kt was not generated or not generated in the expected" +
          "location"
      )
  }
}

enum class SourceType {
  KOTLIN,
  JAVA
}

sealed interface TypedSourceFile {
  fun sourceFile(): SourceFile
  fun sourceType(): SourceType
}

class GeneratedSourceFile(
  private val file: File,
  private val currentSourceType: SourceType,
) : TypedSourceFile {
  override fun sourceFile(): SourceFile = SourceFile.fromPath(file)

  // Hack alert: We use this class only for generated output of some previous compilation. We rely
  // on the type in that previous compilation to select the proper source. The output however is
  // always Kotlin, regardless of source. But we always want to include whatever the generated
  // output is in the next step. That means we need our sourceType here to match the
  //  currentSourceType in the test.
  override fun sourceType(): SourceType = currentSourceType
}

class KotlinSourceFile(
  val name: String,
  @Language("kotlin") val content: String,
) : TypedSourceFile {
  override fun sourceFile() = SourceFile.kotlin(name, content)
  override fun sourceType() = SourceType.KOTLIN
}

class JavaSourceFile(
  val name: String,
  @Language("java") val content: String,
) : TypedSourceFile {
  override fun sourceFile() = SourceFile.java(name, content)
  override fun sourceType() = SourceType.JAVA
}

interface PerSourceTypeTest {
  val sourceType: SourceType

  fun compileCurrentSourceType(
    vararg sourceFiles: TypedSourceFile,
    test: (input: CompilationResult) -> Unit = {},
  ): CompilationResult {
    val result =
      compile(sourceFiles.filter { it.sourceType() == sourceType }.map { it.sourceFile() }.toList())
    test(result)
    return result
  }
}

internal fun compile(sourceFiles: List<SourceFile>): CompilationResult {
  require(sourceFiles.isNotEmpty())
  val compilation =
    KotlinCompilation().apply {
      sources = sourceFiles
      symbolProcessorProviders = listOf(GlideSymbolProcessorProvider())
      inheritClassPath = true
    }
  val result = compilation.compile()
  return CompilationResult(compilation, result)
}

fun StringSubject.hasSourceEqualTo(sourceContents: String) = isEqualTo(sourceContents.trimIndent())

object CommonSources {
  // generated code always includes public and Unit
  @Suppress("RedundantVisibilityModifier", "RedundantUnitReturnType")
  @Language("kotlin")
  const val simpleAppGlideModule =
    """
package com.bumptech.glide

import AppModule
import android.content.Context
import kotlin.Boolean
import kotlin.Suppress
import kotlin.Unit

internal class GeneratedAppGlideModuleImpl(
  @Suppress("UNUSED_PARAMETER")
  context: Context,
) : GeneratedAppGlideModule() {
  private val appGlideModule: AppModule
  init {
    appGlideModule = AppModule()
  }

  public override fun registerComponents(
    context: Context,
    glide: Glide,
    registry: Registry,
  ): Unit {
    appGlideModule.registerComponents(context, glide, registry)
  }

  public override fun applyOptions(context: Context, builder: GlideBuilder): Unit {
    appGlideModule.applyOptions(context, builder)
  }

  public override fun isManifestParsingEnabled(): Boolean = false
}
"""
}
