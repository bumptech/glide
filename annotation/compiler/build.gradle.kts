plugins {
    id("java")
}

dependencies {
    implementation(libs.javapoet)
    implementation(libs.guava)

    compileOnly(libs.autoservice)
    compileOnly(libs.findbugs.jsr305)

    implementation(project(":annotation"))
    annotationProcessor(libs.autoservice)
}

tasks.withType<Javadoc> {
    isFailOnError = false
}

apply(from = "${rootProject.projectDir}/scripts/upload.gradle.kts")