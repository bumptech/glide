plugins {
    id("com.android.library")
}

android {
    namespace = "com.bumptech.glide.integration.avif"
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
    implementation(libs.avif)
    implementation(libs.guava)

    annotationProcessor(project(":annotation:compiler"))
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")