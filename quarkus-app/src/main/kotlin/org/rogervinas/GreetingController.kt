package org.rogervinas

import org.eclipse.microprofile.config.inject.ConfigProperty
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

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
