import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
  kotlin("jvm") version "2.1.21"
  kotlin("plugin.allopen") version "2.1.21"
  id("io.quarkus")
  id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
}

group = "org.rogervinas"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
  mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
  implementation("io.quarkiverse.vault:quarkus-vault")
  implementation("io.quarkus:quarkus-reactive-pg-client")
  implementation("io.quarkus:quarkus-agroal")
  implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
  implementation("io.quarkus:quarkus-kotlin")
  implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("io.quarkus:quarkus-arc")
  implementation("io.quarkus:quarkus-resteasy-reactive")

  implementation("io.quarkus:quarkus-config-yaml")

  implementation("io.quarkus:quarkus-flyway")
  implementation("io.quarkus:quarkus-jdbc-postgresql")

  testImplementation("io.quarkus:quarkus-junit5")
  testImplementation("io.quarkus:quarkus-junit5-mockito")
  testImplementation("io.rest-assured:rest-assured")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks.withType<Test> {
  systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
  testLogging {
    events(PASSED, SKIPPED, FAILED)
    exceptionFormat = FULL
    showExceptions = true
    showCauses = true
    showStackTraces = true
  }
}

allOpen {
  annotation("javax.ws.rs.Path")
  annotation("javax.enterprise.context.ApplicationScoped")
  annotation("io.quarkus.test.junit.QuarkusTest")
}
