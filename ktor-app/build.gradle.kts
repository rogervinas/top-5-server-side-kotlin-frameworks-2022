val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project

plugins {
  kotlin("jvm") version "1.9.20"
  id("io.ktor.plugin") version "2.2.4"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
}

group = "org.rogervinas"
version = "0.0.1"

application {
  mainClass.set("org.rogervinas.GreetingApplicationKt")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

ktor {
  docker {
    localImageName.set("${project.name}")
    imageTag.set("${project.version}")
    jreVersion.set(io.ktor.plugin.features.JreVersion.JRE_17)
  }
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

  implementation("com.bettercloud:vault-java-driver:5.1.0")

  implementation("ch.qos.logback:logback-classic:$logback_version")

  testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")

  testImplementation("io.mockk:mockk:1.13.8")
  testImplementation("org.testcontainers:junit-jupiter:1.19.1")
  testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "17"
  }
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
