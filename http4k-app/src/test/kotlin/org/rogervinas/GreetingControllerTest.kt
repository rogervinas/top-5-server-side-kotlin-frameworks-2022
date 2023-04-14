package org.rogervinas

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class GreetingControllerTest {

  private val controller = createController()

  @Test
  fun `should ping`() {
    val response = controller(Request(GET, "/ping"))
    assertThat(response, hasStatus(Status.OK))
    assertThat(response, hasBody("pong"))
  }
}
