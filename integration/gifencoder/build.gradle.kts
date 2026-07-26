plugins {
    id("com.android.library")
}

android {
    namespace = "com.bumptech.glide.integration.gifencoder"
    compileSdk = libs.versions.compile.sdk.version.get().toInt()

    sourceSets {
        getByName("main") {
            java.directories.addAll(listOf("src/main/java", "../../third_party/gif_encoder/src/main/java"))
        }
    }

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

    testImplementation(project(":testutil"))
    testImplementation(libs.truth)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.runner)
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")