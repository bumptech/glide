package com.bumptech.glide.annotation.ksp.integrationtest

import com.bumptech.glide.annotation.ksp.test.CommonSources
import com.bumptech.glide.annotation.ksp.test.JavaSourceFile
import com.bumptech.glide.annotation.ksp.test.KotlinSourceFile
import com.bumptech.glide.annotation.ksp.test.PerSourceTypeTest
import com.bumptech.glide.annotation.ksp.test.SourceType
import com.bumptech.glide.annotation.ksp.test.hasSourceEqualTo
import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class IntegrationLibraryGlideModuleTests(override val sourceType: SourceType) : PerSourceTypeTest {

  companion object {
    @Parameters(name = "sourceType = {0}") @JvmStatic fun data() = SourceType.values()
  }

  @Test
  fun compile_withOnlyAppGlideModule_generatesGeneratedAppGlideModule_thatCallsDependencyLibraryGlideModules() {
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

    compileCurrentSourceType(
      kotlinAppModule,
      javaAppModule,
    ) {
      assertThat(it.exitCode).isEqualTo(ExitCode.OK)
      assertThat(it.generatedAppGlideModuleContents())
        .hasSourceEqualTo(appGlideModuleWithOnlyDependencyLibraryModules)
    }
  }

  @Test
  fun compile_withValidLibraryGlideModule_andAppGlideModule_generatesGeneratedAppGlideModule_thatCallsAllLibraryAndDependencyAndAppGlideModules() {
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
        .hasSourceEqualTo(appGlideModuleWithLibraryModuleAndDependencyLibraryModules)
    }
  }

  @Test
  fun compile_withDependencyModuleInExcludes_generatesGeneratedAppGlideModule_thatDoesNotCallDependencyLibraryGlideModules() {
    val kotlinAppModule =
      KotlinSourceFile(
        "AppModule.kt",
        """
        import com.bumptech.glide.annotation.Excludes
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule
        import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule

        @Excludes(OkHttpLibraryGlideModule::class) 
        @GlideModule class AppModule : AppGlideModule()
        """
      )
    val javaAppModule =
      JavaSourceFile(
        "AppModule.java",
        """
          import com.bumptech.glide.annotation.Excludes;
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.AppGlideModule;
          import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule;
          
          @Excludes(OkHttpLibraryGlideModule.class)
          @GlideModule 
          public class AppModule extends AppGlideModule {
            public AppModule() {}
          }
        """
      )

    compileCurrentSourceType(
      kotlinAppModule,
      javaAppModule,
    ) {
      assertThat(it.exitCode).isEqualTo(ExitCode.OK)
      assertThat(it.generatedAppGlideModuleContents())
        .hasSourceEqualTo(CommonSources.simpleAppGlideModule)
    }
  }

  @Test
  fun compile_withLibraryModuleInExcludes_producesGeneratedAppGlideModuleThatDoesNotCallExcludedLibraryModule() {
    val kotlinExcludedLibraryModule =
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

    val javaExcludedLibraryModule =
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
      kotlinExcludedLibraryModule,
      javaAppModule,
      javaExcludedLibraryModule
    ) {
      assertThat(it.generatedAppGlideModuleContents())
        .hasSourceEqualTo(appGlideModuleWithOnlyDependencyLibraryModules)
      assertThat(it.exitCode).isEqualTo(ExitCode.OK)
    }
  }
}

// generated code always includes public and Unit
@Suppress("RedundantVisibilityModifier", "RedundantUnitReturnType")
@Language("kotlin")
const val appGlideModuleWithLibraryModuleAndDependencyLibraryModules =
  """
package com.bumptech.glide

import AppModule
import LibraryModule
import android.content.Context
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule
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
    OkHttpLibraryGlideModule().registerComponents(context, glide, registry)
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
const val appGlideModuleWithOnlyDependencyLibraryModules =
  """
package com.bumptech.glide

import AppModule
import android.content.Context
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule
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
    OkHttpLibraryGlideModule().registerComponents(context, glide, registry)
    appGlideModule.registerComponents(context, glide, registry)
  }

  public override fun applyOptions(context: Context, builder: GlideBuilder): Unit {
    appGlideModule.applyOptions(context, builder)
  }

  public override fun isManifestParsingEnabled(): Boolean = false
}
"""
