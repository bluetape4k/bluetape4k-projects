package io.bluetape4k.workshop.movierating

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.tests.withTestContextSuspending
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class MovieRatingVerticeTest {

    companion object: KLogging()

    /**
     * BeforeAll, BeforeEach 에서는 testContext 가 불필요합니다. 만약 injection을 받으면 꼭 completeNow() 를 호출해야 합니다.
     */
    @BeforeAll
    fun beforeAll(vertx: Vertx) {
        runBlocking(vertx.dispatcher()) {
            vertx.deployVerticle(MovieRatingVerticle()).coAwait()
            log.debug { "MovieRatingVerticle deployed." }
        }
    }

    @Test
    fun `get movie by id`(vertx: Vertx, testContext: VertxTestContext) = runSuspendTest {
        vertx.withTestContextSuspending(testContext) {
            val client = WebClient.create(vertx)

            val movieId = "starwars"
            log.debug { "Send movie request for id=$movieId" }
            val response = client
                .get(8080, "localhost", "/movie/$movieId")
                .`as`(BodyCodec.jsonObject())
                .send()
                .coAwait()

            log.debug { "Response body=${response.body()}" }

            response.statusCode() shouldBeEqualTo 200
            val movie = response.body()
            movie.getString("id") shouldBeEqualTo movieId
            movie.getString("title").shouldNotBeEmpty()
        }
    }

    @Test
    fun `get rating`(vertx: Vertx, testContext: VertxTestContext) = runSuspendTest {
        vertx.withTestContextSuspending(testContext) {
            val client = WebClient.create(vertx)

            val movieId = "starwars"
            log.debug { "Send movie request for id=$movieId" }
            val response = client
                .get(8080, "localhost", "/getRating/$movieId")
                .`as`(BodyCodec.jsonObject())
                .send()
                .coAwait()

            log.debug { "Response body=${response.body()}" }

            response.statusCode() shouldBeEqualTo 200
            val movie = response.body()
            movie.getString("id") shouldBeEqualTo movieId
            movie.getDouble("getRating") shouldBeGreaterThan 0.0
        }
    }

    @Test
    fun `post movie rating`(vertx: Vertx, testContext: VertxTestContext) = runSuspendTest {
        vertx.withTestContextSuspending(testContext) {
            val client = WebClient.create(vertx)

            val movieId = "starwars"
            val response = client
                .post(8080, "localhost", "/rateMovie/$movieId")
                .`as`(BodyCodec.string())
                .addQueryParam("getRating", "9")
                .send()
                .coAwait()

            response.statusCode() shouldBeEqualTo 200
        }
    }
}
