import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.9.25"
  id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
  id("com.google.devtools.ksp") version "1.9.25-1.0.20"
  id("com.gradleup.shadow") version "8.3.9"
  id("io.micronaut.application") version "4.6.2"
  id("io.micronaut.test-resources") version "4.6.2"
  id("io.micronaut.aot") version "4.6.2"
  id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

version = "0.1"
group = "org.rogervinas"

val kotlinVersion = project.properties["kotlinVersion"]

repositories {
  mavenCentral()
}

dependencies {
  ksp("io.micronaut:micronaut-http-validation")
  ksp("io.micronaut.data:micronaut-data-processor")
  ksp("io.micronaut.serde:micronaut-serde-processor")
  implementation("io.micronaut.data:micronaut-data-jdbc")
  implementation("io.micronaut.flyway:micronaut-flyway")
  implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
  implementation("io.micronaut.serde:micronaut-serde-jackson")
  implementation("io.micronaut.sql:micronaut-jdbc-hikari")
  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation("io.micronaut.discovery:micronaut-discovery-client")

  implementation("org.postgresql:postgresql:42.5.1")
  implementation("org.jdbi:jdbi3-core:3.36.0")

  implementation("io.micronaut:micronaut-http-client")
  runtimeOnly("org.yaml:snakeyaml")
  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly("org.fusesource.jansi:jansi:2.4.1")
  runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin") {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
  }
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  testImplementation("io.micronaut:micronaut-http-client")
  testImplementation("io.micronaut.test:micronaut-test-rest-assured")
  testImplementation("io.mockk:mockk:1.13.3")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
  mainClass = "org.rogervinas.ApplicationKt"
}

java {
  sourceCompatibility = JavaVersion.toVersion("21")
}

graalvmNative.toolchainDetection = false

micronaut {
  runtime("netty")
  testRuntime("junit5")
  processing {
    incremental(true)
    annotations("org.rogervinas.*")
  }
  aot {
    optimizeServiceLoading = false
    convertYamlToJava = false
    precomputeOperations = true
    cacheEnvironment = true
    optimizeClassLoading = true
    deduceEnvironment = true
    optimizeNetty = true
    replaceLogbackXml = true
  }
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("dockerfile") {
  baseImage.set("eclipse-temurin:21-jre-alpine")
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
  jdkVersion = "21"
}

tasks.withType<Test> {
  testLogging {
    events(PASSED, SKIPPED, FAILED)
    exceptionFormat = FULL
    showExceptions = true
    showCauses = true
    showStackTraces = true
  }
}
