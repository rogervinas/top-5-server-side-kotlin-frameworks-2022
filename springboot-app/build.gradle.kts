plugins {
  id("org.springframework.boot") version "3.1.5"
  id("io.spring.dependency-management") version "1.1.4"
  kotlin("jvm") version "1.9.20"
  kotlin("plugin.spring") version "1.9.20"
}

group = "org.rogervinas"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
  mavenCentral()
}

extra["springCloudVersion"] = "2022.0.4"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.cloud:spring-cloud-starter-vault-config")

  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  // TODO not still compatible with current version of SpringBoot
  // implementation("org.flywaydb:flyway-core:10.0.0")
  // implementation("org.flywaydb:flyway-database-postgresql:10.0.0")
  implementation("org.flywaydb:flyway-core:9.22.3")
  implementation("org.postgresql:postgresql:42.6.0")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.projectreactor:reactor-test")

  testImplementation("org.testcontainers:junit-jupiter:1.17.6")
  testImplementation("org.assertj:assertj-core:3.24.0")
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "21"
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
