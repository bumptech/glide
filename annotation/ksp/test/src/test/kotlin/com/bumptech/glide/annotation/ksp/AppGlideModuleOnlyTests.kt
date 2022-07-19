package com.bumptech.glide.annotation.ksp.test

import com.bumptech.glide.annotation.ksp.AppGlideModuleConstants
import com.bumptech.glide.annotation.ksp.GlideSymbolProcessorConstants
import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class OnlyAppGlideModuleTests(override val sourceType: SourceType) : PerSourceTypeTest {

  companion object {
    @Parameterized.Parameters(name = "sourceType = {0}") @JvmStatic fun data() = SourceType.values()
  }

  @Test
  fun compile_withGlideModuleOnNonLibraryClass_fails() {
    val kotlinSource =
      KotlinSourceFile(
        "Something.kt",
        """
          import com.bumptech.glide.annotation.GlideModule
          @GlideModule class Something 
        """
      )

    val javaSource =
      JavaSourceFile(
        "Something.java",
        """
        package test;
        
        import com.bumptech.glide.annotation.GlideModule;
        @GlideModule
        public class Something {}
        """
      )

    compileCurrentSourceType(kotlinSource, javaSource) {
      assertThat(it.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
      assertThat(it.messages)
        .containsMatch(
          GlideSymbolProcessorConstants.INVALID_ANNOTATED_CLASS.format(".*/Something.*")
        )
    }
  }

  @Test
  fun compile_withGlideModuleOnValidAppGlideModule_generatedGeneratedAppGlideModule() {
    val kotlinModule =
      KotlinSourceFile(
        "Module.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class Module : AppGlideModule()
        """
      )
    val javaModule =
      JavaSourceFile(
        "Module.java",
        """
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.AppGlideModule;
          
          @GlideModule public class Module extends AppGlideModule {}
        """.trimIndent()
      )

    compileCurrentSourceType(kotlinModule, javaModule) {
      assertThat(it.generatedAppGlideModuleContents()).hasSourceEqualTo(simpleAppGlideModule)
      assertThat(it.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
  }

  @Test
  fun compile_withAppGlideModuleConstructorAcceptingOnlyContext_generatesGeneratedAppGlideModule() {
    val kotlinModule =
      KotlinSourceFile(
        "Module.kt",
        """
        import android.content.Context
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class Module(context: Context) : AppGlideModule()
        """
      )

    val javaModule =
      JavaSourceFile(
        "Module.java",
        """
          import android.content.Context;
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.AppGlideModule;
          
          @GlideModule public class Module extends AppGlideModule {
            public Module(Context context) {}
          }
        """
      )

    compileCurrentSourceType(kotlinModule, javaModule) {
      assertThat(it.generatedAppGlideModuleContents()).hasSourceEqualTo(appGlideModuleWithContext)
      assertThat(it.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
  }

  @Test
  fun compile_withAppGlideModuleConstructorRequiringOtherThanContext_fails() {
    val kotlinModule =
      KotlinSourceFile(
        "Module.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class Module(value: Int) : AppGlideModule()
        """
      )
    val javaModule =
      JavaSourceFile(
        "Module.java",
        """
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.AppGlideModule;
          
          @GlideModule public class Module extends AppGlideModule {
            public Module(Integer value) {}
          }
        """
      )

    compileCurrentSourceType(kotlinModule, javaModule) {
      assertThat(it.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
      assertThat(it.messages).contains(AppGlideModuleConstants.INVALID_MODULE_MESSAGE)
    }
  }

  @Test
  fun compile_withAppGlideModuleConstructorRequiringMultipleArguments_fails() {
    val kotlinModule =
      KotlinSourceFile(
        "Module.kt",
        """
        import android.content.Context
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class Module(value: Context, otherValue: Int) : AppGlideModule()
        """
      )
    val javaModule =
      JavaSourceFile(
        "Module.java",
        """
          import android.content.Context;
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.AppGlideModule;
          
          @GlideModule public class Module extends AppGlideModule {
            public Module(Context value, int otherValue) {}
          }
        """
      )

    compileCurrentSourceType(kotlinModule, javaModule) {
      assertThat(it.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
      assertThat(it.messages).contains(AppGlideModuleConstants.INVALID_MODULE_MESSAGE)
    }
  }

  // This is quite weird, we could probably pretty reasonably just assert that this doesn't happen.
  @Test
  fun compile_withAppGlideModuleWithOneEmptyrConstructor_andOneContextOnlyConstructor_usesTheContextOnlyConstructor() {
    val kotlinModule =
      KotlinSourceFile(
        "Module.kt",
        """
        import android.content.Context
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class Module(context: Context?) : AppGlideModule() {
          constructor() : this(null)
        }
        
        """
      )
    val javaModule =
      JavaSourceFile(
        "Module.java",
        """
          import android.content.Context;
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.AppGlideModule;
          import javax.annotation.Nullable;
          
          @GlideModule public class Module extends AppGlideModule {
            public Module() {}
            public Module(@Nullable Context context) {}
          }
        """
      )

    compileCurrentSourceType(kotlinModule, javaModule) {
      assertThat(it.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
      assertThat(it.generatedAppGlideModuleContents()).hasSourceEqualTo(appGlideModuleWithContext)
    }
  }

  @Test
  fun copmile_withMultipleAppGlideModules_failes() {
    val firstKtModule =
      KotlinSourceFile(
        "Module1.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class Module1 : AppGlideModule()
        """
      )

    val secondKtModule =
      KotlinSourceFile(
        "Module2.kt",
        """
        import com.bumptech.glide.annotation.GlideModule
        import com.bumptech.glide.module.AppGlideModule

        @GlideModule class Module2 : AppGlideModule()
        """
      )

    val firstJavaModule =
      JavaSourceFile(
        "Module1.java",
        """
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.AppGlideModule;
          
          @GlideModule public class Module1 extends AppGlideModule {
            public Module1() {}
          }
        """
      )

    val secondJavaModule =
      JavaSourceFile(
        "Module2.java",
        """
          import com.bumptech.glide.annotation.GlideModule;
          import com.bumptech.glide.module.AppGlideModule;
          
          @GlideModule public class Module2 extends AppGlideModule {
            public Module2() {}
          }
        """
      )

    compileCurrentSourceType(firstKtModule, secondKtModule, firstJavaModule, secondJavaModule) {
      assertThat(it.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
      assertThat(it.messages)
        .contains(
          GlideSymbolProcessorConstants.SINGLE_APP_MODULE_ERROR.format("[Module1, Module2]")
        )
    }
  }
}

@Language("kotlin")
const val simpleAppGlideModule =
  """
package com.bumptech.glide

import Module
import android.content.Context
import kotlin.Boolean
import kotlin.Suppress
import kotlin.Unit

internal class GeneratedAppGlideModuleImpl(
  @Suppress("UNUSED_VARIABLE")
  context: Context,
) : GeneratedAppGlideModule() {
  private val appGlideModule: Module
  init {
    appGlideModule = Module()
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

@Language("kotlin")
const val appGlideModuleWithContext =
  """
package com.bumptech.glide

import Module
import android.content.Context
import kotlin.Boolean
import kotlin.Unit

internal class GeneratedAppGlideModuleImpl(
  context: Context,
) : GeneratedAppGlideModule() {
  private val appGlideModule: Module
  init {
    appGlideModule = Module(context)
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
