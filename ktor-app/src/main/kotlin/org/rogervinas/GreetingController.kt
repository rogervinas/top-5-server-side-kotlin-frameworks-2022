package org.rogervinas

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.greetingController(
      name: String,
      secret: String,
      repository: GreetingRepository
) {
  routing {
    get("/hello") {
      call.respondText {
        "${repository.getGreeting()} my name is $name and my secret is $secret"
      }
    }
  }
}
