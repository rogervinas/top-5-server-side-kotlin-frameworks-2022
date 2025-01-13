import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

val ktorVersion: String = project.property("ktor_version") as String
val kotlinVersion: String = project.property("kotlin_version") as String
val logbackVersion: String = project.property("logback_version") as String
val postgresVersion: String = project.property("postgres_version") as String

plugins {
  kotlin("jvm") version "2.1.0"
  id("io.ktor.plugin") version "3.0.3"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
  id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
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
    jreVersion.set(JavaVersion.VERSION_21)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-config-yaml")

  implementation("org.postgresql:postgresql:$postgresVersion")

  implementation("com.bettercloud:vault-java-driver:5.1.0")

  implementation("ch.qos.logback:logback-classic:$logbackVersion")

  testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")

  testImplementation("io.mockk:mockk:1.13.16")
  testImplementation("org.testcontainers:junit-jupiter:1.20.4")
  testImplementation("org.assertj:assertj-core:3.27.2")
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
