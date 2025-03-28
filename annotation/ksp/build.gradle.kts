plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp")
}

sourceSets {
    named("main") {
        java.srcDirs("src/main/java", "src/main/kotlin")
    }
}

dependencies {
    implementation("com.squareup:kotlinpoet:2.1.0")
    implementation(project(":annotation"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.27")
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.2.0")
}

// apply from: "${rootProject.projectDir}/scripts/upload.gradle"

//kotlin {
//    jvmToolchain {
//        languageVersion.set(JavaLanguageVersion.of(11))
//    }
//}
