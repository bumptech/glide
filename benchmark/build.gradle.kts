plugins {
    id("com.android.library")
    id("androidx.benchmark")
}

android {
    namespace = "com.bumptech.glide.benchmark"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    defaultConfig {
        minSdk = 19

        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        multiDexEnabled = true
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "benchmark-proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.multidex)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.junit)

    androidTestImplementation(project(":library"))
    androidTestImplementation(project(":testutil"))
    androidTestImplementation(libs.androidx.benchmark.junit)
    androidTestImplementation(libs.guava)
}