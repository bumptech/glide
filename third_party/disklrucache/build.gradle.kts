plugins {
    id("com.android.library")
    checkstyle
}

checkstyle {
    toolVersion = "6.19"
    configFile = file("checkstyle.xml")
}

android {
    namespace = "com.bumptech.glide.disklrucache"
    compileSdk = libs.versions.compile.sdk.version.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

val uploaderScript = "${rootProject.projectDir}/scripts/upload.gradle.kts"

if (file(uploaderScript).exists()) {
    apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")
}