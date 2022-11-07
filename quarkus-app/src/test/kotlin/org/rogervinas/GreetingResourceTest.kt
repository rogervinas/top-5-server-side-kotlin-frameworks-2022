package org.rogervinas

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.doReturn

@QuarkusTest
class GreetingResourceTest {

    @InjectMock
    private lateinit var repository: GreetingRepository

    @Test
    fun testHelloEndpoint() {
        doReturn("1.2.3").`when`(repository).getVersion()

        given()
          .`when`().get("/hello")
          .then()
             .statusCode(200)
             .body(`is`("Hellouuuu! 1.2.3"))
    }

}