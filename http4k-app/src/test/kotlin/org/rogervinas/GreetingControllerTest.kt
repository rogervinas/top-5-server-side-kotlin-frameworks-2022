package org.rogervinas

import com.natpryce.hamkrest.assertion.assertThat
import io.mockk.every
import io.mockk.mockk
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class GreetingControllerTest {

  private val repository = mockk<GreetingRepository>().apply {
    every { getGreeting() } returns "Hello"
  }

  private val controller = greetingController("Bitelchus", "apple", repository)

  @Test
  fun `should ping`() {
    val response = controller(Request(GET, "/ping"))
    assertThat(response, hasStatus(Status.OK))
    assertThat(response, hasBody("pong"))
  }

  @Test
  fun `should say hello`() {
    val response = controller(Request(GET, "/hello"))
    assertThat(response, hasStatus(Status.OK))
    assertThat(response, hasBody("Hello my name is Bitelchus and my secret is apple"))
  }
}
