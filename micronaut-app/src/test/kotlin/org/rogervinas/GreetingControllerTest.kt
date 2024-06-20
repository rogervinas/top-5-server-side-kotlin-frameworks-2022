package org.rogervinas

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus.OK
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest
@Property(name = "greeting.secret", value = "apple")
class GreetingControllerTest {
  @Inject
  @field:Client("/")
  private lateinit var client: HttpClient

  @Inject
  private lateinit var repository: GreetingRepository

  @Test
  fun `should say hello`() {
    every { repository.getGreeting() } returns "Hello"

    val request: HttpRequest<Any> = HttpRequest.GET("/hello")
    val response = client.toBlocking().exchange(request, String::class.java)

    assertEquals(OK, response.status)
    assertEquals("Hello my name is Bitelchus and my secret is apple", response.body.get())
  }

  @MockBean(GreetingRepository::class)
  fun repository() = mockk<GreetingRepository>()
}
