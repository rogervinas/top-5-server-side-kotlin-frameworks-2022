package org.rogervinas

import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@WebFluxTest
@TestPropertySource(properties = [
  "spring.cloud.vault.enabled=false",
  "greeting.secret=apple"
])
class GreetingControllerTest {

  @MockBean
  private lateinit var repository: GreetingRepository

  @Autowired
  private lateinit var client: WebTestClient

  @Test
  fun `should say hello`() {
    doReturn("Hello").`when`(repository).getGreeting()

    client
          .get().uri("/hello")
          .exchange()
          .expectStatus().isOk
          .expectBody<String>().isEqualTo("Hello my name is Bitelchus and my secret is apple")
  }
}
