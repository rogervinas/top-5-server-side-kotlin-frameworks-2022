package org.rogervinas

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus.OK
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@MicronautTest
class GreetingApplicationTest {

    @Inject
    @field:Client("/")
    private lateinit var client: HttpClient

    @Inject
    private lateinit var repository: GreetingRepository

    @Test
    fun `should say hello`() {
        val request: HttpRequest<Any> = HttpRequest.GET("/hello")
        val response = client.toBlocking().exchange(request, String::class.java)

        assertEquals(OK, response.status)
        assertTrue(response.body.get().matches(Regex(".+ my name is Bitelchus and my secret is unknown")))
    }
}
