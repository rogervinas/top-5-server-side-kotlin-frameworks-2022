import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
  kotlin("jvm") version "2.3.20"
  application
  id("com.gradleup.shadow") version "9.0.0-beta12"
  id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }

  dependencies {
  }
}

val http4kVersion: String by project
val junitVersion: String by project
val kotlinVersion: String by project

application {
  mainClass = "org.rogervinas.GreetingApplicationdKt"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.http4k:http4k-client-okhttp:$http4kVersion")
  implementation("org.http4k:http4k-core:$http4kVersion")
  implementation("org.http4k:http4k-format-jackson:$http4kVersion")
  implementation("org.http4k:http4k-server-undertow:$http4kVersion")
  implementation("org.http4k:http4k-cloudnative:$http4kVersion")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation("org.postgresql:postgresql:42.7.10")

  implementation("com.bettercloud:vault-java-driver:5.1.0")

  testImplementation("org.http4k:http4k-testing-approval:$http4kVersion")
  testImplementation("org.http4k:http4k-testing-hamkrest:$http4kVersion")

  testImplementation(platform("org.junit:junit-bom:$junitVersion"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  testImplementation("io.mockk:mockk:1.14.9")
  testImplementation("org.testcontainers:junit-jupiter:1.21.4")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events(PASSED, SKIPPED, FAILED)
  }
}
