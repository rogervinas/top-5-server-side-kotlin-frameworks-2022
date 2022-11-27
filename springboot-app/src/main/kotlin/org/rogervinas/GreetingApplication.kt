package org.rogervinas

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GreetingApplication

fun main(args: Array<String>) {
	runApplication<GreetingApplication>(*args)
}
