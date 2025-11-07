import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bumptech.glide.integration.compose"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }

    buildTypes { getByName("release") { isMinifyEnabled = false } }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlin.compiler.extension.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) } }

    testOptions { unitTests { isIncludeAndroidResources = true } }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (!name.contains("Test")) {
        kotlinOptions.freeCompilerArgs += "-Xexplicit-api=strict"
    }
}

dependencies {
    implementation(project(":library"))
    implementation(project(":integration:ktx"))

    implementation(project(":integration:recyclerview")) { isTransitive = false }

    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.drawablepainter)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.testmanifest)
    testImplementation(libs.compose.ui.testmanifest)
    testImplementation(libs.compose.ui.testjunit4)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.appcompat)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.lifecycle.runtime.testing)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.compose.ui.testjunit4)
    androidTestImplementation(libs.androidx.espresso)
    androidTestImplementation(libs.androidx.espresso.idling)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.compose.material)
    androidTestImplementation(libs.truth)
    androidTestImplementation(project(":testutil"))
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")
