import com.android.build.gradle.LibraryExtension
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("com.android.library")
}

if (!hasProperty("DISABLE_ERROR_PRONE")) {
    apply(plugin = "net.ltgt.errorprone")
}

tasks.withType<JavaCompile>().configureEach {
    options.setFork(true)
}

dependencies {
    api(project(":third_party:gif_decoder"))
    api(project(":third_party:disklrucache"))
    api(project(":annotation"))
    api(libs.androidx.fragment)
    api(libs.androidx.vectordrawable)
    api(libs.androidx.exifinterface)
    api(libs.androidx.tracing)
    compileOnly(libs.androidx.appcompat)

    if (project.plugins.hasPlugin("net.ltgt.errorprone")) {
        "errorprone"(libs.errorprone.core)
    }

    testImplementation(libs.androidx.appcompat)
    testImplementation(project(":testutil"))
    testImplementation(libs.guava.testlib)
    testImplementation(libs.truth)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.runner)
}

if (project.plugins.hasPlugin("net.ltgt.errorprone")) {
    tasks.withType<JavaCompile>().configureEach {
        options.errorprone.disable(
            // It's often useful to track individual objects when debugging
            // object pooling.
            "ObjectToString",
            // Doesn't apply when we can't use lambadas.
            "UnnecessaryAnonymousClass",
            // TODO(judds): Fix these and re-enable this check
            "BadImport",
            "UnescapedEntity",
            "MissingSummary",
            "InlineMeSuggester",
            "CanIgnoreReturnValueSuggester",
            "TypeNameShadowing",
            "UndefinedEquals",
            "UnnecessaryParentheses",
            "UnusedVariable",
            "EqualsGetClass",
            "LockNotBeforeTry",
        )
    }
}

android {
    namespace = "com.bumptech.glide"
    compileSdkVersion = libs.versions.compile.sdk.version.get()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()
        consumerProguardFiles("proguard-rules.txt")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// Change the name to make it a little more obvious where the main library
// documentation has gone. Using a capital letter happens to make this first in
// the list too...
afterEvaluate {
    tasks.named("dokkaHtmlPartial").configure {
        (this as org.jetbrains.dokka.gradle.DokkaTaskPartial).dokkaSourceSets {
            named("main") {
                moduleName.set("Glide")
            }
        }
    }
}

tasks.named("check") {
    dependsOn(":library:pmd:pmd")
    dependsOn(":library:test:check")
}

// Used in pmd and findbugs subprojects.
val classPathForQuality by extra {
    files(android.bootClasspath) +
        project.the<LibraryExtension>().libraryVariants.map { it.javaCompileProvider.get().classpath }
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")
