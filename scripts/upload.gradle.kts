// TODO(b/409147894): Should be documented properly

import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withGroovyBuilder

pluginManager.apply("com.vanniktech.maven.publish")

fun prop(key: String): String? = if (hasProperty(key)) property(key)?.toString() else null

val groupId: String = prop("GROUP") ?: group.toString()
val artifactId: String = prop("POM_ARTIFACT_ID") ?: name
val versionName: String = prop("VERSION_NAME") ?: version.toString()
val publishVariant: String = prop("PUBLISH_VARIANT") ?: "release"
val isSnapshot: Boolean = versionName.endsWith("SNAPSHOT")

if (pluginManager.hasPlugin("com.android.library")) {
  extensions.findByName("android")?.withGroovyBuilder {
    "publishing" {
      "singleVariant"(publishVariant) {
        "withSourcesJar"()
        "withJavadocJar"()
      }
    }
  }
}

if (pluginManager.hasPlugin("java") || pluginManager.hasPlugin("java-library")) {
  extensions.configure(org.gradle.api.plugins.JavaPluginExtension::class.java) {
    withSourcesJar()
    // TODO(b/409147894): Put this back after explicit Dokka task dependency
    // withJavadocJar()
  }
}

extensions.findByName("mavenPublishing")?.withGroovyBuilder {
  val autoRelease = prop("MAVEN_CENTRAL_AUTO_RELEASE")?.toBoolean() ?: false
  val wantsCustomRepo =
    prop("LOCAL") != null ||
      prop("RELEASE_REPOSITORY_URL") != null ||
      prop("SNAPSHOT_REPOSITORY_URL") != null

  if (!wantsCustomRepo && autoRelease) {
    "publishToMavenCentral"(mapOf("automaticRelease" to true))
  }

  if (!wantsCustomRepo && !autoRelease) {
    "publishToMavenCentral"()
  }

  if (!isSnapshot) {
    val hasSigning =
      hasProperty("signingInMemoryKey") ||
        hasProperty("signing.secretKeyRingFile") ||
        hasProperty("signing.keyId")
    if (hasSigning) {
      "signAllPublications"()
    }
  }

  "pom" {
    "licenses" {
      "license" {
        setProperty("name", "Simplified BSD License")
        setProperty("url", "http://www.opensource.org/licenses/bsd-license")
        setProperty("distribution", "repo")
      }
      "license" {
        setProperty("name", "Apache License, Version 2.0")
        setProperty("url", "http://www.apache.org/licenses/LICENSE-2.0.txt")
        setProperty("distribution", "repo")
      }
    }

    "developers" {
      "developer" {
        setProperty("id", prop("POM_DEVELOPER_ID"))
        setProperty("name", prop("POM_DEVELOPER_NAME"))
        setProperty("email", prop("POM_DEVELOPER_EMAIL"))
      }
    }

    "scm" {
      setProperty("connection", prop("POM_SCM_CONNECTION"))
      setProperty("developerConnection", prop("POM_SCM_DEV_CONNECTION"))
      setProperty("url", prop("POM_SCM_URL"))
    }
  }
}
