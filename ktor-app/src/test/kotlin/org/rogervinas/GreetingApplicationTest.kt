package org.rogervinas

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.*
import org.assertj.core.api.Assertions.assertThat
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait.forLogMessage
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import kotlin.test.Test

@Testcontainers
class GreetingApplicationTest {

  companion object {
    @Container
    private val container = DockerComposeContainer(File("../docker-compose.yaml"))
          .withServices("db", "vault", "vault-cli")
          .withLocalCompose(true)
          .waitingFor("db", forLogMessage(".*database system is ready to accept connections.*", 1))
          .waitingFor("vault", forLogMessage(".*Development mode.*", 1))
  }

  @Test
  fun `should say hello`() = testApplication {
    client.get("/hello").apply {
      assertThat(status).isEqualTo(OK)
      assertThat(bodyAsText()).matches(".+ my name is Bitelchus and my secret is unknown")
    }
  }
}
