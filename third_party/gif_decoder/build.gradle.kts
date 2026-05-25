plugins {
    id("com.android.library")
}

android {
    namespace = "com.bumptech.glide.gifdecoder"
    compileSdkVersion = libs.versions.compile.sdk.version.get()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()
    }
}

dependencies {
    implementation(libs.androidx.annotation)

    testImplementation(project(":testutil"))
    testImplementation(libs.androidx.annotation)
    testImplementation(libs.truth)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")