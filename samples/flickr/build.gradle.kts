plugins {
    id("com.android.application")
}

android {
    namespace = "com.bumptech.glide.samples.flickr"
    compileSdkVersion = libs.versions.compile.sdk.version.get()

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

    implementation(project(":integration:recyclerview")) {
        isTransitive = false
    }

    annotationProcessor(project(":annotation:compiler"))

    implementation(libs.androidx.appcompat)
    implementation(libs.volley)
    implementation(libs.androidx.recyclerview)
}