plugins {
    id("java")
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}