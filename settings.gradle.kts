import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "glide-parent"

include(":library")

include(":library:pmd")

include(":library:test")

include(":instrumentation")

include(":annotation")

include(":annotation:compiler")

// include(":annotation:compiler:test")
include(":annotation:ksp")

include(":annotation:ksp:test")

include(":annotation:ksp:integrationtest")

include(":benchmark")

include(":glide")

include(":third_party:gif_decoder")

include(":third_party:disklrucache")

include(":samples:flickr")

include(":samples:giphy")

include(":samples:svg")

include(":samples:gallery")

include(":samples:contacturi")

include(":samples:imgur")

include(":integration")

include(":integration:avif")

include(":integration:compose")

include(":integration:concurrent")

include(":integration:cronet")

include(":integration:gifencoder")

include(":integration:ktx")

include(":integration:okhttp")

include(":integration:okhttp3")

include(":integration:okhttp4")

include(":integration:recyclerview")

include(":integration:sqljournaldiskcache")

include(":integration:volley")

include(":testutil")

include(":mocks")
