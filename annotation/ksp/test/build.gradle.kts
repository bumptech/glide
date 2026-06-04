plugins {
    id("com.android.library")
}

android {
    namespace = "com.bumptech.glide.annotation.ksp.test"
    compileSdk = libs.versions.compile.sdk.version.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(libs.junit)
    implementation(project(":annotation:ksp"))
    implementation(libs.ksp.compiletesting)
    implementation(libs.truth)

    testImplementation(project(":annotation:ksp"))
    testImplementation(project(":annotation"))
    testImplementation(project(":glide"))
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test>().configureEach {
    enabled = false
}
