import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(ktorLibs.plugins.ktor)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ktlint)
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

dependencies {
  implementation(ktorLibs.server.core)
  implementation(ktorLibs.server.netty)
  implementation(ktorLibs.serialization.kotlinx.json)
  implementation(ktorLibs.server.contentNegotiation)
  implementation(ktorLibs.server.config.yaml)

  implementation(libs.postgresql)

  implementation(libs.vault.java.driver)

  implementation(libs.logback.classic)

  testImplementation(kotlin("test"))
  testImplementation(ktorLibs.server.testHost)

  testImplementation(libs.mockk)
  testImplementation(libs.testcontainers.junit.jupiter)
  testImplementation(libs.assertj.core)
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
