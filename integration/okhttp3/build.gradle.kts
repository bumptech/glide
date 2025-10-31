plugins {
    id("com.android.library")
}

android {
    namespace = "com.bumptech.glide.integration.okhttp"
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
    annotationProcessor(project(":annotation:compiler"))

    api(libs.okhttp3)
    api(libs.androidx.annotation)
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")