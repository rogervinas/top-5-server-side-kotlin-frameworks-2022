package org.rogervinas

import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.restassured.RestAssured.`when`
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn

@QuarkusTest
@TestHTTPEndpoint(GreetingController::class)
class GreetingControllerTest {
  @InjectMock
  private lateinit var repository: GreetingRepository

  @Test
  fun `should say hello`() {
    doReturn("Hello").`when`(repository).getGreeting()

    `when`()
      .get()
      .then()
      .statusCode(200)
      .body(`is`("Hello my name is Bitelchus and my secret is watermelon"))
  }
}
