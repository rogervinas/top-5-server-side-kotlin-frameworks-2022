package org.rogervinas

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
