import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  application
  id("com.gradleup.shadow") version "9.4.1"
  id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

application {
  mainClass = "org.rogervinas.GreetingApplicationKt"
}

tasks {
  shadowJar {
    archiveBaseName.set(project.name)
    archiveClassifier = null
    archiveVersion = null
    mergeServiceFiles()
    dependsOn(distTar, distZip)
    isZip64 = true
  }
}

repositories {
  mavenCentral()
}

tasks {
  withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
      allWarningsAsErrors.set(false)
      jvmTarget.set(JVM_21)
      freeCompilerArgs.add("-jvm-default=enable")
    }
  }

  withType<Test> {
    useJUnitPlatform()
    testLogging {
      events(PASSED, SKIPPED, FAILED)
    }
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

dependencies {
  implementation(platform("org.http4k:http4k-bom:6.46.0.0"))
  implementation("org.http4k:http4k-client-okhttp")
  implementation("org.http4k:http4k-config")
  implementation("org.http4k:http4k-core")
  implementation("org.http4k:http4k-format-jackson")
  implementation("org.http4k:http4k-server-undertow")

  implementation("org.postgresql:postgresql:42.7.11")
  implementation("com.bettercloud:vault-java-driver:5.1.0")

  testImplementation("org.http4k:http4k-testing-approval")
  testImplementation("org.http4k:http4k-testing-hamkrest")

  testImplementation(platform("org.junit:junit-bom:6.0.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  testImplementation("io.mockk:mockk:1.14.9")
  testImplementation("org.testcontainers:junit-jupiter:1.21.4")
}
