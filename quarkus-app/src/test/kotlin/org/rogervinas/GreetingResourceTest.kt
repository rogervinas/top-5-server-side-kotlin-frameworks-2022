package org.rogervinas

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn

@QuarkusTest
class GreetingResourceTest {

    @InjectMock
    private lateinit var repository: GreetingRepository

    @Test
    fun testHelloEndpoint() {
        doReturn("Hello").`when`(repository).getGreeting()

        given()
          .`when`().get("/hello")
          .then()
             .statusCode(200)
             .body(`is`("Hello Bitelchus"))
    }

}