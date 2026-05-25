plugins {
    id("com.android.library")
}

android {
    namespace = "com.bumptech.glide.testutil"
    compileSdkVersion = libs.versions.compile.sdk.version.get()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.truth)
    implementation(project(":library"))
    api(libs.androidx.annotation)
    api(libs.androidx.core)
    api(libs.androidx.test.core)
}