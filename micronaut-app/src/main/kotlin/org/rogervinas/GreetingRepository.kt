package org.rogervinas

import jakarta.inject.Singleton

interface GreetingRepository {
    fun getGreeting(): String
}

@Singleton
class GreetingSimpleRepository : GreetingRepository {
    override fun getGreeting() = "Hola"
}