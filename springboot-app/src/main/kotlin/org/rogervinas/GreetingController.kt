package org.rogervinas

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/hello")
class GreetingController(
        private val repository: GreetingRepository,
        @Value("\${greeting.name}") private val name: String,
        @Value("\${greeting.secret}") private val secret: String
) {

    @GetMapping(produces = [MediaType.TEXT_PLAIN_VALUE])
    fun hello() = "${repository.getGreeting()} my name is $name and my secret is $secret"
}