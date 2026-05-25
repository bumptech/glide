plugins {
    id("com.android.library")
}

android {
    namespace = "com.bumptech.glide.integration.concurrent"
    compileSdkVersion = libs.versions.compile.sdk.version.get()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":library"))
    implementation(libs.guava)
    implementation(libs.androidx.futures)

    testImplementation(project(":mocks"))
    testImplementation(project(":testutil"))
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.truth)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")