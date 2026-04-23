import com.android.build.gradle.LibraryExtension

plugins {
    pmd
}

val library = project(":library")

pmd {
    toolVersion = libs.versions.pmd.get()
}

tasks.register<Pmd>("pmd") {
    dependsOn(library.tasks.named("compileReleaseJavaWithJavac"))
    targetJdk = TargetJdk.VERSION_1_7

    description = "Run pmd"
    group = "verification"

    // If ruleSets is not empty, it seems to contain some
    // defaults which override rules in the ruleset file...
    ruleSets = emptyList()
    ruleSetFiles = files("${library.projectDir}/pmd-ruleset.xml")
    source(library.the<LibraryExtension>().sourceSets.getByName("main").java.srcDirs)
    classpath = files(library.tasks.named("compileReleaseJavaWithJavac").map { (it as org.gradle.api.tasks.compile.JavaCompile).destinationDirectory })
    doFirst {
        classpath = classpath!!.plus(library.extra["classPathForQuality"] as FileCollection)
    }

    //TODO enable this once new Gradle containing this flag is out
    //see https://github.com/gradle/gradle/pull/3125#issuecomment-352442432
    //incrementalAnalysis = true

    // Failures are caught and printed by the violations plugin.
    ignoreFailures = true

    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}
