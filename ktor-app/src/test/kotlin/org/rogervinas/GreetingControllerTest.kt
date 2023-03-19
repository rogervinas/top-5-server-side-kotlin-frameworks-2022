package org.rogervinas

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class GreetingControllerTest {

  private val repository = mockk<GreetingRepository>()

  @Test
  fun `should say hello`() = testApplication {
    environment {
      config = MapApplicationConfig()
    }
    application {
      every { repository.getGreeting() } returns "Hello"
      greetingController("Bitelchus", "apple", repository)
    }
    client.get("/hello").apply {
      assertThat(status).isEqualTo(OK)
      assertThat(bodyAsText()).isEqualTo("Hello my name is Bitelchus and my secret is apple")
    }
  }
}
