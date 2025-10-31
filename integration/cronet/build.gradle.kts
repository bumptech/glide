plugins {
    id("com.android.library")
}

android {
    namespace = "com.bumptech.glide.integration.cronet"
    compileSdkVersion = libs.versions.compile.sdk.version.get()

    defaultConfig {
        minSdk = 16
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":library"))
    implementation(libs.cronet)
    implementation(libs.guava)
    implementation(project(":annotation"))
    annotationProcessor(project(":annotation:compiler"))

    api(libs.androidx.annotation)

    testImplementation(libs.truth)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")