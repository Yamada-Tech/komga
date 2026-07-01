import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.jreleaser.model.Active
import org.jreleaser.model.Distribution.DistributionType.SINGLE_JAR
import org.jreleaser.model.api.common.Apply
import kotlin.io.path.Path
import kotlin.io.path.exists

plugins {
  run {
    val kotlinVersion = "2.2.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("kapt") version kotlinVersion
  }
  id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
  id("com.github.ben-manes.versions") version "0.52.0"
  id("org.jreleaser") version "1.19.0"
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
  val unstableKeyword = listOf("ALPHA", "RC").any { version.uppercase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return unstableKeyword || !isStable
}

group = "org.gotson"

allprojects {
  repositories {
    mavenCentral()
  }
  apply(plugin = "org.jlleitschuh.gradle.ktlint")
  apply(plugin = "com.github.ben-manes.versions")

  tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    // disallow release candidates as upgradable versions from stable versions
    rejectVersionIf {
      isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
    gradleReleaseChannel = "current"
    checkConstraints = true
  }

  configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version = "1.7.1"
    filter {
      exclude("**/generated-src/**")
      exclude("**/generated/**")
    }
  }
}

tasks.wrapper {
  gradleVersion = "8.14.3"
  distributionType = Wrapper.DistributionType.ALL
}

val portableRootDir = layout.buildDirectory.dir("portable")
val portableStageDir = portableRootDir.map { it.dir("stage") }
val portableAppImageDir = portableRootDir.map { it.dir("app-image") }
val portableInputDir = portableRootDir.map { it.dir("input") }
val runKomgaBatFile = portableRootDir.map { it.file("run-komga.bat") }
val komgaBootJarFile = project(":komga").layout.buildDirectory.file("libs/komga-${project.version}.jar")

val shadowJar by tasks.registering {
  group = "build"
  description = "Build frontend and backend executable JAR for portable packaging"
  dependsOn(":komga:prepareThymeLeaf", ":komga:bootJar")
}

val generateRunKomgaBat by tasks.registering {
  group = "distribution"
  description = "Generate launcher for portable Windows package"
  outputs.file(runKomgaBatFile)
  doLast {
    runKomgaBatFile.get().asFile.apply {
      parentFile.mkdirs()
      writeText(
        """
        |@echo off
        |cd /d "%~dp0"
        |start "" "http://localhost:25600"
        |start "" ".\KomgaCustom.exe"
        """.trimMargin(),
      )
    }
  }
}

val createWindowsPortableAppImage by tasks.registering(Exec::class) {
  group = "distribution"
  description = "Create Windows app image used in portable ZIP"
  dependsOn(shadowJar)
  onlyIf { Os.isFamily(Os.FAMILY_WINDOWS) }
  inputs.file(komgaBootJarFile)
  outputs.dir(portableAppImageDir)
  doFirst {
    delete(portableAppImageDir, portableInputDir)
    val inputDir = portableInputDir.get().asFile.apply { mkdirs() }
    copy {
      from(komgaBootJarFile)
      into(inputDir)
    }
    commandLine(
      "jpackage",
      "--type",
      "app-image",
      "--name",
      "KomgaCustom",
      "--input",
      inputDir.absolutePath,
      "--main-jar",
      komgaBootJarFile.get().asFile.name,
      "--dest",
      portableAppImageDir.get().asFile.absolutePath,
    )
  }
}

val preparePortableLayout by tasks.registering(Sync::class) {
  group = "distribution"
  description = "Prepare files for portable ZIP"
  dependsOn(shadowJar, generateRunKomgaBat, createWindowsPortableAppImage)
  into(portableStageDir)
  from(runKomgaBatFile)
  if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    from(portableAppImageDir.map { it.dir("KomgaCustom") })
  } else {
    from(komgaBootJarFile)
  }
}

tasks.register<Zip>("createPortableZip") {
  group = "distribution"
  description = "Create a Windows portable ZIP distribution"
  dependsOn(preparePortableLayout)
  from(portableStageDir)
  destinationDirectory.set(layout.buildDirectory.dir("distributions"))
  archiveFileName.set("komga-portable-${project.version}.zip")
}

jreleaser {
  project {
    description = "Media server for comics/mangas/BDs with API and OPDS support"
    copyright = "Gauthier Roebroeck"
    authors.add("Gauthier Roebroeck")
    license = "MIT"
    links {
      homepage = "https://komga.org"
    }
  }

  release {
    github {
      discussionCategoryName = "Announcements"
      skipTag = true
      tagName = "{{projectVersion}}"

      changelog {
        formatted = Active.ALWAYS
        preset = "conventional-commits"
        skipMergeCommits = true
        links = true
        content = (if (Path("./release_notes/release_notes.md").exists()) "{{#f_file_read}}{{basedir}}/release_notes/release_notes.md{{/f_file_read}}" else "") +
          """
          ## Changelog

          {{changelogChanges}}
          {{changelogContributors}}
          """.trimIndent()
        format = "- {{#commitIsConventional}}{{#conventionalCommitIsBreakingChange}}🚨 {{/conventionalCommitIsBreakingChange}}{{#conventionalCommitScope}}**{{conventionalCommitScope}}**: {{/conventionalCommitScope}}{{conventionalCommitDescription}}{{#conventionalCommitBreakingChangeContent}}: *{{conventionalCommitBreakingChangeContent}}*{{/conventionalCommitBreakingChangeContent}} ({{commitShortHash}}){{/commitIsConventional}}{{^commitIsConventional}}{{commitTitle}} ({{commitShortHash}}){{/commitIsConventional}}{{#commitHasIssues}}, closes{{#commitIssues}} {{issue}}{{/commitIssues}}{{/commitHasIssues}}"
        hide {
          uncategorized = true
          contributors = listOf("Weblate", "GitHub", "semantic-release-bot", "[bot]", "github-actions")
        }
        excludeLabels.add("chore")
        category {
          title = "🏎 Perf"
          key = "perf"
          labels.add("perf")
          order = 25
        }
        category {
          title = "🌐 Translation"
          key = "i18n"
          labels.add("i18n")
          order = 70
        }
        category {
          title = "⚙️ Dependencies"
          key = "dependencies"
          labels.add("dependencies")
          order = 80
        }
        labeler {
          label = "perf"
          title = "regex:^(?:perf(?:\\(.*\\))?!?):\\s.*"
          order = 120
        }
        labeler {
          label = "i18n"
          title = "regex:^(?:i18n(?:\\(.*\\))?!?):\\s.*"
          order = 130
        }
        labeler {
          label = "dependencies"
          title = "regex:^(?:deps(?:\\(.*\\))?!?):\\s.*"
          order = 140
        }
        extraProperties.put("categorizeScopes", true)
        append {
          enabled = true
          title = "# [{{projectVersion}}]({{repoUrl}}/compare/{{previousTagName}}...{{tagName}}) ({{#f_now}}YYYY-MM-dd{{/f_now}})"
          target = rootDir.resolve("CHANGELOG.md")
          content =
            """
            {{changelogTitle}}
            {{changelogChanges}}
            """.trimIndent()
        }
      }

      issues {
        enabled = true
        comment = "🎉 This issue has been resolved in `{{tagName}}` ([Release Notes]({{releaseNotesUrl}}))"
        applyMilestone = Apply.ALWAYS
        label {
          name = "released"
          description = "Issue has been released"
          color = "#ededed"
        }
      }
    }
  }

  distributions {
    create("komga") {
      active = Active.RELEASE
      distributionType = SINGLE_JAR
      artifact {
        path = rootDir.resolve("komga/build/libs/komga-{{projectVersion}}.jar")
      }
    }
  }

  packagers {
    docker {
      active = Active.RELEASE
      continueOnError = false
      templateDirectory = rootDir.resolve("komga/docker")
      repository.active = Active.NEVER
      buildArgs = listOf("--cache-from", "gotson/komga:latest")
      imageNames =
        listOf(
          "komga:latest",
          "komga:{{projectVersion}}",
          "komga:{{projectVersionMajor}}.x",
        )
      registries {
        create("docker.io") { externalLogin = true }
        create("ghcr.io") { externalLogin = true }
      }
      buildx {
        enabled = true
        createBuilder = false
        platforms =
          listOf(
            "linux/amd64",
            "linux/arm/v7",
            "linux/arm64/v8",
          )
      }
    }
  }
}
