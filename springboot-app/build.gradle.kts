import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
  id("org.springframework.boot") version "3.4.3"
  id("io.spring.dependency-management") version "1.1.7"
  kotlin("jvm") version "2.1.10"
  kotlin("plugin.spring") version "2.1.10"
}

group = "org.rogervinas"
version = "0.0.1-SNAPSHOT"

repositories {
  mavenCentral()
}

val springCloudVersion = "2024.0.0"
val flywayVersion = "11.3.4"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.cloud:spring-cloud-starter-vault-config")

  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("org.flywaydb:flyway-core:$flywayVersion")
  implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
  implementation("org.postgresql:postgresql:42.7.5")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.projectreactor:reactor-test")

  testImplementation("org.testcontainers:junit-jupiter:1.20.5")
  testImplementation("org.assertj:assertj-core:3.27.3")
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
  }
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
