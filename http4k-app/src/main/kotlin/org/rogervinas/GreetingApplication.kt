package org.rogervinas

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer

fun createController() = routes(
  "/ping" bind GET to {
    Response(OK).body("pong")
  }
)

fun createServer(port: Int) = PrintRequest().then(createController()).asServer(Undertow(port))

fun main() {
  val server = createServer(8080)
  server.start()
  println("Server started on " + server.port())
}
