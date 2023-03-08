import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date

fun properties(key: String) = project.findProperty(key)?.toString()

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.7.20"
  id("org.jetbrains.intellij") version "1.13.0"
  id("org.jetbrains.changelog") version "2.0.0"
}

group = "com.gitee"
version = "${properties("pluginVersion")}.${properties("pluginBuildNumber")}"

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

configurations {
  implementation {
    resolutionStrategy.failOnVersionConflict()
  }
}

intellij {
  version.set(properties("ideaVersion"))

  pluginName.set("intellij-gitee")
  plugins.set(listOf("tasks", "Git4Idea"))

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
      provider {
        changelog.renderItem(
          changelog.getOrNull("${project.version}")?.withHeader(false) ?: changelog.getLatest().withHeader(false),
          Changelog.OutputType.HTML
        )
      }
    )
  }

  compileKotlin {
    kotlinOptions.jvmTarget = "17"
  }

  compileTestKotlin {
    kotlinOptions.jvmTarget = "17"
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