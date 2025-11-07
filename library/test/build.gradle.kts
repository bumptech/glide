import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("com.android.library")
}

tasks.withType<JavaCompile>().configureEach {
    options.setFork(true)
}

android {
    (this as LibraryExtension).testOptions.unitTests.all { testTask ->
        testTask.maxHeapSize = rootProject.extra.get("TEST_JVM_MEMORY_SIZE") as String

        if (JavaVersion.current() <= JavaVersion.VERSION_1_8) {
            testTask.jvmArgs("-XX:MaxPermSize=${rootProject.extra.get("TEST_JVM_MEMORY_SIZE")}")
        }

        testTask.maxParallelForks = 2
    }

    namespace = "com.bumptech.glide.test"
    compileSdkVersion = libs.versions.compile.sdk.version.get()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    sourceSets {
        getByName("androidTest") {
            resources.srcDirs(files("../../exifsamples"))
        }
        getByName("test") {
            resources.srcDirs(files("../../exifsamples"))
        }
    }
}

dependencies {
    testImplementation(libs.androidx.appcompat)
    testImplementation(project(":library"))
    testImplementation(project(":mocks"))
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

afterEvaluate {
    tasks.named("lint").configure { enabled = false }
    tasks.named("compileReleaseJavaWithJavac").configure { enabled = false }
    tasks.named("compileDebugJavaWithJavac").configure { enabled = false }
}