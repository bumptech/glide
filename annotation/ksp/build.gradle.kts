plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp")
}

kotlin { jvmToolchain { languageVersion.set(JavaLanguageVersion.of(11)) } }

dependencies {
    implementation(libs.kotlinpoet)
    implementation(project(":annotation"))
    implementation(libs.ksp.api)
    implementation(libs.autoservice.annotations)

    ksp(libs.ksp.autoservice)
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")
