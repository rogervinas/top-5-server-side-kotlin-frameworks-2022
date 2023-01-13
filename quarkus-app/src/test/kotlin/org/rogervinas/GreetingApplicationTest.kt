package org.rogervinas

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test

@QuarkusTest
class GreetingApplicationTest {

  @Test
  fun `should say hello`() {
    given()
          .`when`().get("/hello")
          .then()
          .statusCode(200)
          .body(matchesPattern(".+ my name is Bitelchus and my secret is watermelon"))
  }
}
