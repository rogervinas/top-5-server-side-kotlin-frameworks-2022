import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

val ktorVersion = "3.4.0"
val kotlinVersion = "2.3.10"
val logbackVersion = "1.4.11"
val postgresVersion = "42.5.1"

plugins {
  kotlin("jvm") version "2.3.10"
  id("io.ktor.plugin") version "3.4.0"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10"
  id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
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
    localImageName.set(project.name)
    imageTag.set(project.version.toString())
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
  implementation("io.ktor:ktor-server-config-yaml:$ktorVersion")

  implementation("org.postgresql:postgresql:$postgresVersion")

  implementation("com.bettercloud:vault-java-driver:5.1.0")

  implementation("ch.qos.logback:logback-classic:$logbackVersion")

  testImplementation("io.ktor:ktor-server-tests-jvm:2.3.13")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")

  testImplementation(platform("org.junit:junit-bom:6.0.3"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.junit.jupiter:junit-jupiter")

  testImplementation("io.mockk:mockk:1.14.9")
  testImplementation("org.testcontainers:junit-jupiter:1.21.4")
  testImplementation("org.assertj:assertj-core:3.27.7")
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
