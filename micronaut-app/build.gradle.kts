plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.22"
  id("org.jetbrains.kotlin.plugin.allopen") version "1.8.22"
  id("com.google.devtools.ksp") version "1.8.22-1.0.11"
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("io.micronaut.application") version "4.0.4"
  id("io.micronaut.test-resources") version "4.0.4"
  id("io.micronaut.aot") version "4.0.4"
}

version = "0.1"
group = "org.rogervinas"

val kotlinVersion = project.properties["kotlinVersion"]

repositories {
  mavenCentral()
}

dependencies {
  ksp("io.micronaut:micronaut-http-validation")
  compileOnly("io.micronaut:micronaut-http-client")
  implementation("io.micronaut:micronaut-jackson-databind")
  implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
  implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
  runtimeOnly("ch.qos.logback:logback-classic")

  implementation("jakarta.annotation:jakarta.annotation-api")
  implementation("io.micronaut:micronaut-validation")

  implementation("io.micronaut.discovery:micronaut-discovery-client")

  ksp("io.micronaut.data:micronaut-data-processor")
  implementation("io.micronaut.data:micronaut-data-jdbc")
  implementation("io.micronaut.flyway:micronaut-flyway")
  implementation("io.micronaut.sql:micronaut-jdbc-hikari")
  implementation("org.postgresql:postgresql:42.5.1")
  implementation("org.jdbi:jdbi3-core:3.36.0")

  runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

  testImplementation("io.micronaut.test:micronaut-test-rest-assured")
  testImplementation("io.mockk:mockk:1.13.3")
}

application {
  mainClass.set("org.rogervinas.ApplicationKt")
}

java {
  sourceCompatibility = JavaVersion.toVersion("17")
}

tasks {
  compileKotlin {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
  compileTestKotlin {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
}

graalvmNative.toolchainDetection.set(false)

micronaut {
  runtime("netty")
  testRuntime("junit5")
  processing {
    incremental(true)
    annotations("org.rogervinas.*")
  }
  aot {
    // Please review carefully the optimizations enabled below
    // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
    optimizeServiceLoading.set(false)
    convertYamlToJava.set(false)
    precomputeOperations.set(true)
    cacheEnvironment.set(true)
    optimizeClassLoading.set(true)
    deduceEnvironment.set(true)
    optimizeNetty.set(true)
  }
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("dockerfile") {
  baseImage.set("eclipse-temurin:17-jre-alpine")
}

tasks.withType<Test> {
  testLogging {
    events(
      org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
      org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
      org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
    )
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showExceptions = true
    showCauses = true
    showStackTraces = true
  }
}
