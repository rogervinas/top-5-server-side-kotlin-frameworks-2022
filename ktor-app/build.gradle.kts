val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project

plugins {
  kotlin("jvm") version "1.8.10"
  id("io.ktor.plugin") version "2.2.4"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
}

group = "org.rogervinas"
version = "0.0.1"

application {
  mainClass.set("org.rogervinas.GreetingApplicationKt")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
  implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
  implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
  implementation("io.ktor:ktor-server-config-yaml")

  implementation("org.postgresql:postgresql:$postgres_version")

  implementation("ch.qos.logback:logback-classic:$logback_version")

  testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")

  testImplementation("io.mockk:mockk:1.13.4")
  testImplementation("org.testcontainers:junit-jupiter:1.17.6")
  testImplementation("org.assertj:assertj-core:3.24.0")
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events(
          org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
          org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
          org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
    )
  }
}
