plugins {
    id("com.android.application")
}

android {
    namespace = "com.bumptech.glide.samples.svg"
    compileSdk = libs.versions.compile.sdk.version.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()
        

        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":library"))
    annotationProcessor(project(":annotation:compiler"))
    implementation(libs.svg)
    implementation(libs.androidx.fragment)
}