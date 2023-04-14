package org.rogervinas

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.OkHttp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GreetingApplicationTest {

  private val client = OkHttp()
  private val server = createServer(0)

  @BeforeEach
  fun start() {
    server.start()
  }

  @AfterEach
  fun stop() {
    server.stop()
  }

  @Test
  fun `should ping`() {
    val response = client(Request(GET, "http://localhost:${server.port()}/ping"))
    assertThat(response, hasStatus(OK))
    assertThat(response, hasBody("pong"))
  }
}
