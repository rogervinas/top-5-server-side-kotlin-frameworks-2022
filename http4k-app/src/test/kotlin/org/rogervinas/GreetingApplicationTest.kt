package org.rogervinas

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.OkHttp
import org.http4k.cloudnative.env.MapEnvironment
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.rogervinas.GreetingApplication.Companion.SERVER_PORT
import org.rogervinas.GreetingApplication.Companion.greetingApplication
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait.forLogMessage
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.util.Properties

@Testcontainers
class GreetingApplicationTest {

  companion object {
    @Container
    private val container = DockerComposeContainer(File("../docker-compose.yaml"))
      .withServices("db", "vault", "vault-cli")
      .withLocalCompose(true)
      .waitingFor("db", forLogMessage(".*database system is ready to accept connections.*", 1))
      .waitingFor("vault", forLogMessage(".*Development mode.*", 1))
      .waitingFor("vault-cli", forLogMessage(".*created_time.*", 1))
  }

  private val client = OkHttp()
  private val application = greetingApplication(
    MapEnvironment.from(
      Properties().apply {
        this[SERVER_PORT] = 0
      }
    )
  )

  @BeforeEach
  fun start() {
    application.start()
  }

  @AfterEach
  fun stop() {
    application.stop()
  }

  @Test
  fun `should say hello`() {
    val response = client(Request(GET, "http://localhost:${application.port()}/hello"))
    assertThat(response, hasStatus(OK))
    assertThat(response, hasBody(Regex(".+ my name is Bitelchus and my secret is unknown")))
  }
}
