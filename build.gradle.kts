import org.jetbrains.changelog.date

fun properties(key: String) = project.findProperty(key)?.toString()

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.6.20"
  id("org.jetbrains.intellij") version "1.5.2"
  id("org.jetbrains.changelog") version "1.3.1"
}

group = "com.gitee"
version = "${properties("pluginVersion")}.${properties("pluginBuildNumber")}"

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

configurations {
  implementation {
    resolutionStrategy.failOnVersionConflict()
  }
}

intellij {
  version.set(properties("ideaVersion"))

  pluginName.set("intellij-gitee")
  plugins.set(listOf("tasks", "git4idea"))

  downloadSources.set(!System.getenv("CI_BUILD").toBoolean())

}

tasks {
  buildSearchableOptions {
    enabled = false
  }

  publishPlugin {
    token.set(properties("publishToken"))
    channels.set(listOf("stable"))
  }

  patchPluginXml {
    version.set("${project.version}")
    sinceBuild.set(properties("ideaBuildVersion"))
    untilBuild.set("${properties("ideaBuildVersion")}.*")
    changeNotes.set(
      provider { changelog.getOrNull("${project.version}")?.toHTML() ?: changelog.getLatest().toHTML() }
    )
  }

  compileKotlin {
    kotlinOptions.jvmTarget = "11"
  }

  compileTestKotlin {
    kotlinOptions.jvmTarget = "11"
  }
}

changelog {
  version.set("${project.version}")
  path.set("${project.projectDir}/CHANGELOG.md")
  header.set(provider { "[${version.get()}] - ${date()}" })
  itemPrefix.set("-")
  keepUnreleasedSection.set(true)
  unreleasedTerm.set("[Unreleased]")
}