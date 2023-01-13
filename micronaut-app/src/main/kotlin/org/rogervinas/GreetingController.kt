package org.rogervinas

import io.micronaut.context.annotation.Property
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces

@Controller("/hello")
class GreetingController(
      private val repository: GreetingRepository,
      @Property(name = "greeting.name") private val name: String,
      @Property(name = "greeting.secret", defaultValue = "unknown") private val secret: String
) {
  @Get
  @Produces(MediaType.TEXT_PLAIN)
  fun hello() = "${repository.getGreeting()} my name is $name and my secret is $secret"
}
