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
    compileSdkVersion = libs.versions.compile.sdk.version.get()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()

        consumerProguardFiles("proguard-rules.txt")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

val uploaderScript = "${rootProject.projectDir}/scripts/upload.gradle.kts"

if (file(uploaderScript).exists()) {
    apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")
}