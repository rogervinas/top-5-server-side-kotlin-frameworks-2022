package org.rogervinas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait.forLogMessage
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class GreetingApplicationTest {
  companion object {
    @Container
    private val container =
      ComposeContainer(File("../docker-compose.yaml"))
        .withServices("db", "vault", "vault-cli")
        .withLocalCompose(true)
        .waitingFor("db", forLogMessage(".*database system is ready to accept connections.*", 1))
        .waitingFor("vault", forLogMessage(".*Development mode.*", 1))
        .waitingFor("vault-cli", forLogMessage(".*created_time.*", 1))
  }

  @Autowired
  private lateinit var client: WebTestClient

  @Test
  fun `should say hello`() {
    client
      .get().uri("/hello")
      .exchange()
      .expectStatus().isOk
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody!!).matches(".+ my name is Bitelchus and my secret is watermelon")
      }
  }
}
