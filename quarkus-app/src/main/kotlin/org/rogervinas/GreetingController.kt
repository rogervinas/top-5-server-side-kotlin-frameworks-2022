package org.rogervinas

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.config.inject.ConfigProperty

@Path("/hello")
class GreetingController(
  private val repository: GreetingRepository,
  @ConfigProperty(name = "greeting.name") private val name: String,
  @ConfigProperty(name = "greeting.secret", defaultValue = "unknown") private val secret: String,
) {
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  fun hello() = "${repository.getGreeting()} my name is $name and my secret is $secret"
}
