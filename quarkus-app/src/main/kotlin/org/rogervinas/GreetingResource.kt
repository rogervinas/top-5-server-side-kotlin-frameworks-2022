package org.rogervinas

import org.eclipse.microprofile.config.inject.ConfigProperty
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/hello")
class GreetingResource {

    @Inject
    private lateinit var repository: GreetingRepository

    @ConfigProperty(name = "greeting.name")
    private lateinit var name: String

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = "${repository.getGreeting()} $name"
}