package org.rogervinas

import jakarta.inject.Singleton

@Singleton
open class GreetingRepository {

    fun getGreeting() = "Hola"
    //.query("SELECT greeting FROM greetings ORDER BY random() limit 1")
}