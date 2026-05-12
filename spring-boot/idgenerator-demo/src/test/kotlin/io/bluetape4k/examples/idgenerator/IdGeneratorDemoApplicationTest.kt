package io.bluetape4k.examples.idgenerator

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.examples.idgenerator.controller.ErrorResponse
import io.bluetape4k.examples.idgenerator.controller.GeneratorsResponse
import io.bluetape4k.examples.idgenerator.controller.HealthResponse
import io.bluetape4k.examples.idgenerator.controller.IdBatchResponse
import io.bluetape4k.examples.idgenerator.controller.IdResponse
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.utils.Runtimex
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.util.concurrent.ConcurrentHashMap

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "bluetape4k.id-generator.default-batch-size=5",
        "bluetape4k.id-generator.max-batch-size=20",
    ],
)
class IdGeneratorDemoApplicationTest {

    @LocalServerPort
    private val port: Int = 0

    private val client: WebTestClient by lazy {
        WebTestClient
            .bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `explicit id endpoints generate ids`() {
        val endpoints =
            mapOf(
                "/ids/uuid-v4" to "uuid-v4",
                "/ids/uuid-v7" to "uuid-v7",
                "/ids/ulid" to "ulid",
                "/ids/ksuid" to "ksuid",
                "/ids/snowflake" to "snowflake",
                "/ids/flake" to "flake",
            )

        endpoints.forEach { (endpoint, type) ->
            getOk<IdResponse>(endpoint)
                .also {
                    it.type shouldBeEqualTo type
                    it.id.shouldNotBeBlank()
                }
        }
    }

    @Test
    fun `generic idgen endpoint generates ids`() {
        getOk<IdResponse>("/idgen/uuid-v7")
            .also {
                it.type shouldBeEqualTo "uuid-v7"
                it.id.shouldNotBeBlank()
            }
    }

    @Test
    fun `batch endpoints generate unique ids`() {
        val legacyResponse = getOk<IdBatchResponse>("/ids/uuid-v7/batch?size=7")
        val genericResponse = getOk<IdBatchResponse>("/idgen/snowflake/batch?size=7")

        listOf(legacyResponse, genericResponse).forEach { response ->
            response.size shouldBeEqualTo 7
            response.ids shouldHaveSize 7
            response.ids.distinct() shouldHaveSize 7
            response.ids.all { id -> id.isNotBlank() }.shouldBeTrue()
        }
    }

    @Test
    fun `default batch size comes from properties`() {
        getOk<IdBatchResponse>("/idgen/ulid/batch")
            .also {
                it.type shouldBeEqualTo "ulid"
                it.size shouldBeEqualTo 5
                it.ids shouldHaveSize 5
                it.ids.distinct() shouldHaveSize 5
            }
    }

    @Test
    fun `generators and health endpoints expose supported types`() {
        val generatorResponse = getOk<GeneratorsResponse>("/generators")
        val healthResponse = getOk<HealthResponse>("/health")

        val expectedTypes = setOf("uuid-v4", "uuid-v7", "ulid", "ksuid", "snowflake", "flake")

        generatorResponse
            .generators
            .map { it.type }
            .toSet() shouldBeEqualTo expectedTypes

        healthResponse.status shouldBeEqualTo "UP"
        healthResponse.supportedTypes shouldBeEqualTo expectedTypes
    }

    @Test
    fun `invalid type and batch size return bad request`() {
        val typeResponse = getBadRequest("/idgen/unknown")
        val sizeResponse = getBadRequest("/idgen/uuid-v7/batch?size=21")

        typeResponse.status shouldBeEqualTo HttpStatus.BAD_REQUEST.value()
        typeResponse.message.contains("Unsupported generator type").shouldBeTrue()

        sizeResponse.status shouldBeEqualTo HttpStatus.BAD_REQUEST.value()
        sizeResponse.message.contains("Batch size must be between 1 and 20").shouldBeTrue()
    }

    @Test
    fun `parallel requests generate unique uuid v7 and snowflake ids`() =
        runSuspendIO {
            val ids = ConcurrentHashMap<String, Boolean>()

            SuspendedJobTester()
                .workers(Runtimex.availableProcessors)
                .rounds(Runtimex.availableProcessors)
                .add {
                    val id = getOk<IdResponse>("/idgen/uuid-v7").id
                    id.shouldNotBeBlank()
                    ids.putIfAbsent(id, true).shouldBeNull()
                }
                .add {
                    val id = getOk<IdResponse>("/idgen/snowflake").id
                    id.shouldNotBeBlank()
                    ids.putIfAbsent(id, true).shouldBeNull()
                }
                .run()
        }

    private inline fun <reified T: Any> getOk(path: String): T =
        client
            .httpGet(path, HttpStatus.OK)
            .expectBody<T>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

    private fun getBadRequest(path: String): ErrorResponse =
        client
            .httpGet(path, HttpStatus.BAD_REQUEST)
            .expectBody<ErrorResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()
}
