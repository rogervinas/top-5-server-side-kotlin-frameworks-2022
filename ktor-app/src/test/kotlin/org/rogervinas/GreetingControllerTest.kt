package org.rogervinas

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

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
      assertEquals(HttpStatusCode.OK, status)
      assertEquals("Hello my name is Bitelchus and my secret is apple", bodyAsText())
    }
  }
}
