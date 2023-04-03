package com.bumptech.glide.annotation.ksp.test

import com.bumptech.glide.annotation.ksp.AppGlideModuleConstants
import com.bumptech.glide.annotation.ksp.GlideSymbolProcessorConstants
import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import java.io.FileNotFoundException
import kotlin.test.assertFailsWith
import org.intellij.lang.annotations.Language
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class LibraryGlideModuleTests(override val sourceType: SourceType) : PerSourceTypeTest {

  companion object {
    @Parameters(name = "sourceType = {0}") @JvmStatic fun data() = SourceType.values()
  }

  @Test
  fun compile_withAnnotatedAndValidLibraryGlideModule_succeeds_butDoesNotGenerateGeneratedAppGlideModule() {
    val kotlinModule =
      KotlinSourceFile(
        "Module.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class Module : LibraryGlideModule()
        """
      )
    val javaModule =
      JavaSourceFile(
        "Module.java",
        """
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.LibraryGlideModule;
          
          @GlideModule public class Module extends LibraryGlideModule {}
        """
      )

    compileCurrentSourceType(kotlinModule, javaModule) {
      assertThat(it.exitCode).isEqualTo(ExitCode.OK)
      assertFailsWith<FileNotFoundException> { it.generatedAppGlideModuleContents() }
    }
  }

  @Test
  fun compile_withValidLibraryGlideModule_andAppGlideModule_generatesGeneratedAppGlideModule_andCallsBothLibraryAndAppGlideModules() {
    val kotlinLibraryModule =
      KotlinSourceFile(
        "LibraryModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class LibraryModule : LibraryGlideModule()
        """
      )
    val kotlinAppModule =
      KotlinSourceFile(
        "AppModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class AppModule : AppGlideModule()
        """
      )
    val javaLibraryModule =
      JavaSourceFile(
        "LibraryModule.java",
        """
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.LibraryGlideModule;
          
          @GlideModule public class LibraryModule extends LibraryGlideModule {}
        """
      )
    val javaAppModule =
      JavaSourceFile(
        "AppModule.java",
        """
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.AppGlideModule;
          
          @GlideModule public class AppModule extends AppGlideModule {
            public AppModule() {}
          }
        """
      )

    compileCurrentSourceType(
      kotlinAppModule,
      kotlinLibraryModule,
      javaAppModule,
      javaLibraryModule
    ) {
      assertThat(it.exitCode).isEqualTo(ExitCode.OK)
      assertThat(it.generatedAppGlideModuleContents())
        .hasSourceEqualTo(appGlideModuleWithLibraryModule)
    }
  }

  @Test
  fun compile_withMultipleLibraryGlideModules_andAppGlideModule_callsAllLibraryGlideModulesFromGeneratedAppGlideModule() {
    val kotlinLibraryModule1 =
      KotlinSourceFile(
        "LibraryModule1.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class LibraryModule1 : LibraryGlideModule()
        """
      )
    val kotlinLibraryModule2 =
      KotlinSourceFile(
        "LibraryModule2.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class LibraryModule2 : LibraryGlideModule()
        """
      )
    val kotlinAppModule =
      KotlinSourceFile(
        "AppModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class AppModule : AppGlideModule()
        """
      )
    val javaLibraryModule1 =
      JavaSourceFile(
        "LibraryModule1.java",
        """
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.LibraryGlideModule;
        
        @GlideModule public class LibraryModule1 extends LibraryGlideModule {}
        """
      )
    val javaLibraryModule2 =
      JavaSourceFile(
        "LibraryModule2.java",
        """
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.LibraryGlideModule;
        
        @GlideModule public class LibraryModule2 extends LibraryGlideModule {}
        """
      )
    val javaAppModule =
      JavaSourceFile(
        "AppModule.java",
        """
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.AppGlideModule;
        
        @GlideModule public class AppModule extends AppGlideModule {
          public AppModule() {}
        }
        """
      )

    compileCurrentSourceType(
      kotlinAppModule,
      kotlinLibraryModule1,
      kotlinLibraryModule2,
      javaAppModule,
      javaLibraryModule1,
      javaLibraryModule2,
    ) {
      assertThat(it.generatedAppGlideModuleContents())
        .hasSourceEqualTo(appGlideModuleWithMultipleLibraryModules)
      assertThat(it.exitCode).isEqualTo(ExitCode.OK)
    }
  }

  @Test
  fun compile_withTheSameLibraryGlideModuleInMultipleFiles_andAnAppGlideModule_generatesGeneratedAppGlideModuleThatCallsTheLibraryGlideModuleOnce() {
    // Kotlin seems fine with multiple identical classes. For Java this is compile time error
    // already, so we don't have to handle it.
    assumeTrue(sourceType == SourceType.KOTLIN)
    val kotlinLibraryModule1 =
      KotlinSourceFile(
        "LibraryModule1.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class LibraryModule : LibraryGlideModule()
        """
      )
    val kotlinLibraryModule2 =
      KotlinSourceFile(
        "LibraryModule2.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class LibraryModule : LibraryGlideModule()
        """
      )
    val kotlinAppModule =
      KotlinSourceFile(
        "AppModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class AppModule : AppGlideModule()
        """
      )

    compileCurrentSourceType(
      kotlinAppModule,
      kotlinLibraryModule1,
      kotlinLibraryModule2,
    ) {
      assertThat(it.generatedAppGlideModuleContents())
        .hasSourceEqualTo(appGlideModuleWithLibraryModule)
      assertThat(it.exitCode).isEqualTo(ExitCode.OK)
      assertThat(it.messages)
        .contains(
          GlideSymbolProcessorConstants.DUPLICATE_LIBRARY_MODULE_ERROR.format("[LibraryModule]")
        )
    }
  }

  @Test
  fun compile_withLibraryGlideModulesWithDifferentPackages_butSameName_andAppGlideModule_callsEachLibraryGlideModuleOnceFromGeneratedAppGlideModule() {
    // TODO(judds): The two java classes don't compile when run by the annotation processor, which
    //  means we can't really test this case for java code. Fix compilation issue and re-enable this
    //  test for Java code.
    assumeTrue(sourceType == SourceType.KOTLIN)
    val kotlinLibraryModule1 =
      KotlinSourceFile(
        "LibraryModule1.kt",
        """
        package first_package

        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class LibraryModule : LibraryGlideModule()
        """
      )
    val kotlinLibraryModule2 =
      KotlinSourceFile(
        "LibraryModule2.kt",
        """
        package second_package

        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class LibraryModule : LibraryGlideModule()
        """
      )
    val kotlinAppModule =
      KotlinSourceFile(
        "AppModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class AppModule : AppGlideModule()
        """
      )
    val javaLibraryModule1 =
      JavaSourceFile(
        "LibraryModule1.java",
        """
        package first_package;
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.LibraryGlideModule;
        
        public class LibraryModule1 {
          @GlideModule public static final class LibraryModule extends LibraryGlideModule {}
        }
        """
      )
    val javaLibraryModule2 =
      JavaSourceFile(
        "LibraryModule2.java",
        """
        package second_package;
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.LibraryGlideModule;
        
        public class LibraryModule2 {
          @GlideModule public static final class LibraryModule extends LibraryGlideModule {}
        }
        """
      )
    val javaAppModule =
      JavaSourceFile(
        "AppModule.java",
        """
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.AppGlideModule;
        
        @GlideModule public class AppModule extends AppGlideModule {
          public AppModule() {}
        }
        """
      )

    compileCurrentSourceType(
      kotlinAppModule,
      kotlinLibraryModule1,
      kotlinLibraryModule2,
      javaAppModule,
      javaLibraryModule1,
      javaLibraryModule2,
    ) {
      assertThat(it.generatedAppGlideModuleContents())
        .hasSourceEqualTo(appGlideModuleWithPackagePrefixedLibraryModules)
      assertThat(it.exitCode).isEqualTo(ExitCode.OK)
    }
  }

  @Test
  fun compile_withLibraryModuleInExcludes_producesGeneratedAppGlideModuleThatDoesNotCallExcludedLibraryModule() {
    val kotlinLibraryModule1 =
      KotlinSourceFile(
        "LibraryModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class LibraryModule : LibraryGlideModule()
        """
      )
    val kotlinLibraryModule2 =
      KotlinSourceFile(
        "ExcludedLibraryModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class ExcludedLibraryModule : LibraryGlideModule()
        """
      )
    val kotlinAppModule =
      KotlinSourceFile(
        "AppModule.kt",
        """
        import com.bumptech.glide.annotation.Excludes
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule 
        @Excludes(ExcludedLibraryModule::class) 
        class AppModule : AppGlideModule()
        """
      )

    val javaLibraryModule1 =
      JavaSourceFile(
        "LibraryModule.java",
        """
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.LibraryGlideModule;
        
        @GlideModule
        public class LibraryModule extends LibraryGlideModule {}
        """
      )
    val javaLibraryModule2 =
      JavaSourceFile(
        "ExcludedLibraryModule.java",
        """
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.LibraryGlideModule;
        
        @GlideModule
        public class ExcludedLibraryModule extends LibraryGlideModule {}
        """
      )
    val javaAppModule =
      JavaSourceFile(
        "AppModule.java",
        """
        import com.bumptech.glide.annotation.Excludes;
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.AppGlideModule;
        
        @GlideModule 
        @Excludes(ExcludedLibraryModule.class)
        public class AppModule extends AppGlideModule {
          public AppModule() {}
        }
        """
      )
    compileCurrentSourceType(
      kotlinAppModule,
      kotlinLibraryModule1,
      kotlinLibraryModule2,
      javaAppModule,
      javaLibraryModule1,
      javaLibraryModule2,
    ) {
      assertThat(it.generatedAppGlideModuleContents())
        .hasSourceEqualTo(appGlideModuleWithLibraryModule)
      assertThat(it.exitCode).isEqualTo(ExitCode.OK)
    }
  }

  @Test
  fun compile_withMultipleLibraryModulesInExcludes_producesGeneratedAppGlideModuleThatDoesNotCallExcludedLibraryModules() {
    val kotlinLibraryModule1 =
      KotlinSourceFile(
        "LibraryModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class LibraryModule : LibraryGlideModule()
        """
      )
    val kotlinLibraryModule2 =
      KotlinSourceFile(
        "ExcludedLibraryModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class ExcludedLibraryModule : LibraryGlideModule()
        """
      )
    val kotlinAppModule =
      KotlinSourceFile(
        "AppModule.kt",
        """
        import com.bumptech.glide.annotation.Excludes
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule 
        @Excludes(LibraryModule::class, ExcludedLibraryModule::class) 
        class AppModule : AppGlideModule()
        """
      )

    val javaLibraryModule1 =
      JavaSourceFile(
        "LibraryModule.java",
        """
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.LibraryGlideModule;
        
        @GlideModule
        public class LibraryModule extends LibraryGlideModule {}
        """
      )
    val javaLibraryModule2 =
      JavaSourceFile(
        "ExcludedLibraryModule.java",
        """
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.LibraryGlideModule;
        
        @GlideModule
        public class ExcludedLibraryModule extends LibraryGlideModule {}
        """
      )
    val javaAppModule =
      JavaSourceFile(
        "AppModule.java",
        """
        import com.bumptech.glide.annotation.Excludes;
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.AppGlideModule;
        
        @GlideModule 
        @Excludes({LibraryModule.class, ExcludedLibraryModule.class})
        public class AppModule extends AppGlideModule {
          public AppModule() {}
        }
        """
      )
    compileCurrentSourceType(
      kotlinAppModule,
      kotlinLibraryModule1,
      kotlinLibraryModule2,
      javaAppModule,
      javaLibraryModule1,
      javaLibraryModule2,
    ) {
      assertThat(it.generatedAppGlideModuleContents())
        .hasSourceEqualTo(CommonSources.simpleAppGlideModule)
      assertThat(it.exitCode).isEqualTo(ExitCode.OK)
    }
  }

  @Test
  fun compile_withAppModuleWithEmptyExcludes_fails() {
    val kotlinAppModule =
      KotlinSourceFile(
        "AppModule.kt",
        """
        import com.bumptech.glide.annotation.Excludes
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule 
        @Excludes
        class AppModule : AppGlideModule()
        """
      )
    val javaAppModule =
      JavaSourceFile(
        "AppModule.java",
        """
        import com.bumptech.glide.annotation.Excludes;
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.AppGlideModule;
        
        @GlideModule 
        @Excludes
        public class AppModule extends AppGlideModule {
          public AppModule() {}
        }
        """
      )
    compileCurrentSourceType(
      kotlinAppModule,
      javaAppModule,
    ) {
      assertThat(it.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
      assertThat(it.messages)
        .contains(AppGlideModuleConstants.INVALID_EXCLUDES_ANNOTATION_MESSAGE.format("AppModule"))
    }
  }

  @Test
  fun compile_withAppModuleWithExcludes_pointingToAppModules_fails() {
    val kotlinAppModule =
      KotlinSourceFile(
        "AppModule.kt",
        """
        import com.bumptech.glide.annotation.Excludes
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        class SomeOtherAppModule: AppGlideModule()

        @GlideModule 
        @Excludes(SomeOtherAppModule::class)
        class AppModule : AppGlideModule()
        """
      )
    val otherJavaAppModule =
      JavaSourceFile(
        "SomeOtherAppModule.java",
        """
        import com.bumptech.glide.module.AppGlideModule;
        
        public class SomeOtherAppModule extends AppGlideModule {
          public SomeOtherAppModule() {}
        }
        """
      )
    val javaAppModule =
      JavaSourceFile(
        "AppModule.java",
        """
        import com.bumptech.glide.annotation.Excludes;
        import com.bumptech.glide.annotation.GlideModule;
        import com.bumptech.glide.module.AppGlideModule;
        
        @GlideModule 
        @Excludes(SomeOtherAppModule.class)
        public class AppModule extends AppGlideModule {
          public AppModule() {}
        }
        """
      )
    compileCurrentSourceType(
      kotlinAppModule,
      otherJavaAppModule,
      javaAppModule,
    ) {
      assertThat(it.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
      assertThat(it.messages)
        .contains(AppGlideModuleConstants.INVALID_EXCLUDES_ANNOTATION_MESSAGE.format("AppModule"))
    }
  }

  @Test
  fun compile_withLibraryGlideModule_compiledSeparately_includesLibraryGlideModule_2() {
    val kotlinLibraryModule =
      KotlinSourceFile(
        "LibraryModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.LibraryGlideModule

        @GlideModule class LibraryModule : LibraryGlideModule()
        """
      )
    val javaLibraryModule =
      JavaSourceFile(
        "LibraryModule.java",
        """
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.LibraryGlideModule;
          
          @GlideModule public class LibraryModule extends LibraryGlideModule {}
        """
      )

    val libraryCompilationResult = compileCurrentSourceType(kotlinLibraryModule, javaLibraryModule)

    val kotlinAppModule =
      KotlinSourceFile(
        "AppModule.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class AppModule : AppGlideModule()
        """
      )
    val javaAppModule =
      JavaSourceFile(
        "AppModule.java",
        """
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.AppGlideModule;
          
          @GlideModule public class AppModule extends AppGlideModule {
            public AppModule() {}
          }
        """
      )

    val generatedLibrarySources =
      libraryCompilationResult.allGeneratedFiles().map { GeneratedSourceFile(it, sourceType) }

    compileCurrentSourceType(
      *(listOf(kotlinAppModule, javaAppModule) + generatedLibrarySources).toTypedArray(),
    ) {
      assertThat(it.generatedAppGlideModuleContents())
        .hasSourceEqualTo(appGlideModuleWithLibraryModule)
    }
  }
}

// generated code always includes public and Unit
@Suppress("RedundantVisibilityModifier", "RedundantUnitReturnType")
@Language("kotlin")
const val appGlideModuleWithPackagePrefixedLibraryModules =
  """
package com.bumptech.glide

import AppModule
import android.content.Context
import first_package.LibraryModule
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
    LibraryModule().registerComponents(context, glide, registry)
    second_package.LibraryModule().registerComponents(context, glide, registry)
    appGlideModule.registerComponents(context, glide, registry)
  }

  public override fun applyOptions(context: Context, builder: GlideBuilder): Unit {
    appGlideModule.applyOptions(context, builder)
  }

  public override fun isManifestParsingEnabled(): Boolean = false
}
"""

// generated code always includes public and Unit
@Suppress("RedundantVisibilityModifier", "RedundantUnitReturnType")
@Language("kotlin")
const val appGlideModuleWithLibraryModule =
  """
package com.bumptech.glide

import AppModule
import LibraryModule
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
    LibraryModule().registerComponents(context, glide, registry)
    appGlideModule.registerComponents(context, glide, registry)
  }

  public override fun applyOptions(context: Context, builder: GlideBuilder): Unit {
    appGlideModule.applyOptions(context, builder)
  }

  public override fun isManifestParsingEnabled(): Boolean = false
}
"""

// generated code always includes public and Unit
@Suppress("RedundantVisibilityModifier", "RedundantUnitReturnType")
@Language("kotlin")
const val appGlideModuleWithMultipleLibraryModules =
  """
package com.bumptech.glide

import AppModule
import LibraryModule1
import LibraryModule2
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
    LibraryModule1().registerComponents(context, glide, registry)
    LibraryModule2().registerComponents(context, glide, registry)
    appGlideModule.registerComponents(context, glide, registry)
  }

  public override fun applyOptions(context: Context, builder: GlideBuilder): Unit {
    appGlideModule.applyOptions(context, builder)
  }

  public override fun isManifestParsingEnabled(): Boolean = false
}
"""
