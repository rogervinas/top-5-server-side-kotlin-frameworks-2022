package org.rogervinas

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

fun greetingController(
  name: String,
  secret: String,
  repository: GreetingRepository
) = routes(
  "/ping" bind GET to {
    Response(Status.OK).body("pong")
  },
  "/hello" bind GET to {
    Response(Status.OK)
      .body("${repository.getGreeting()} my name is $name and my secret is $secret")
  }
)
