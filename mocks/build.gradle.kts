plugins {
    id("com.android.library")
}

android {
    namespace = "com.bumptech.glide.mocks"

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
    implementation(libs.androidx.annotation)
    implementation(libs.guava)
    implementation(libs.mockito.core)
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")