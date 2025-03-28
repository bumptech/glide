plugins {
    id("java")
}
// apply plugin: 'java'

// apply from: "${rootProject.projectDir}/scripts/upload.gradle"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}