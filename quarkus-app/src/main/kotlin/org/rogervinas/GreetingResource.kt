package org.rogervinas

import org.eclipse.microprofile.config.inject.ConfigProperty
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/hello")
class GreetingResource {

    @ConfigProperty(name = "greeting.message")
    private lateinit var message: String

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = message
}