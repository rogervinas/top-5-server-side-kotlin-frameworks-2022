package org.rogervinas

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.specification.RequestSpecification
import org.hamcrest.text.MatchesPattern.matchesPattern
import org.junit.jupiter.api.Test

@MicronautTest
class GreetingApplicationTest {

    @Test
    fun `should say hello`(spec: RequestSpecification) {
        spec
                .`when`()
                .get("/hello")
                .then()
                .statusCode(200)
                .body(matchesPattern(".+ my name is Bitelchus and my secret is watermelon"))
    }
}
