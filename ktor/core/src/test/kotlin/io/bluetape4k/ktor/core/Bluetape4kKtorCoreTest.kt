package io.bluetape4k.ktor.core

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.Serializable as JavaSerializable

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Bluetape4kKtorCoreTest {

    private val json = Bluetape4kKtorJson.defaultJson()

    @Test
    fun `installer provides json health and readiness defaults`() = testApplication {
        application {
            installBluetape4kKtorCore()
            routing {
                get("/echo") {
                    call.respond(EchoResponse("blue"))
                }
            }
        }

        val health = json.decodeFromString<HealthResponse>(client.get("/healthz").bodyAsText())
        val readiness = json.decodeFromString<HealthResponse>(client.get("/readyz").bodyAsText())
        val echo = json.decodeFromString<EchoResponse>(client.get("/echo").bodyAsText())

        health.status shouldBeEqualTo HealthResponse.UP
        readiness.status shouldBeEqualTo HealthResponse.UP
        echo.value shouldBeEqualTo "blue"
    }

    @Test
    fun `status pages map illegal arguments to bad request payloads`() = testApplication {
        application {
            installBluetape4kKtorCore()
            routing {
                get("/bad") {
                    throw IllegalArgumentException("Invalid input")
                }
            }
        }

        val response = client.get("/bad")
        val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())

        response.status shouldBeEqualTo HttpStatusCode.BadRequest
        body.error shouldBeEqualTo "bad_request"
        body.message shouldBeEqualTo "Invalid input"
        body.status shouldBeEqualTo HttpStatusCode.BadRequest.value
        body.path shouldBeEqualTo "/bad"
    }

    @Test
    fun `status pages hide unhandled exception messages`() = testApplication {
        application {
            installBluetape4kKtorCore()
            routing {
                get("/failure") {
                    throw IllegalStateException("secret-token")
                }
            }
        }

        val response = client.get("/failure")
        val body = json.decodeFromString<ApiErrorResponse>(response.bodyAsText())

        response.status shouldBeEqualTo HttpStatusCode.InternalServerError
        body.error shouldBeEqualTo "internal_server_error"
        body.message shouldBeEqualTo "Internal server error"
        body.path shouldBeEqualTo "/failure"
    }

    @Test
    fun `query parameter helpers validate missing malformed and ranged values`() = testApplication {
        application {
            installBluetape4kKtorCore()
            routing {
                get("/items/{type}") {
                    call.respond(
                        QueryResponse(
                            type = call.requiredPathParameter("type"),
                            filter = call.requiredQueryParameter("filter"),
                            size = call.intQueryParameter("size", defaultValue = 10, range = 1..100)
                        )
                    )
                }
            }
        }

        val ok = json.decodeFromString<QueryResponse>(
            client.get("/items/book?filter=recent&size=7").bodyAsText()
        )
        ok.type shouldBeEqualTo "book"
        ok.filter shouldBeEqualTo "recent"
        ok.size shouldBeEqualTo 7

        val defaultSize = json.decodeFromString<QueryResponse>(
            client.get("/items/book?filter=recent").bodyAsText()
        )
        defaultSize.size shouldBeEqualTo 10

        client.get("/items/book?size=7").status shouldBeEqualTo HttpStatusCode.BadRequest
        client.get("/items/book?filter=recent&size=abc").status shouldBeEqualTo HttpStatusCode.BadRequest
        client.get("/items/book?filter=recent&size=101").status shouldBeEqualTo HttpStatusCode.BadRequest
    }

    @Test
    fun `config rejects non absolute health paths`() {
        assertFailsWith<IllegalArgumentException> {
            Bluetape4kKtorCoreConfig(healthPath = "healthz")
        }
        assertFailsWith<IllegalArgumentException> {
            Bluetape4kKtorCoreConfig(readinessPath = "readyz")
        }
    }

    @Serializable
    private data class EchoResponse(
        val value: String,
    ): JavaSerializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    @Serializable
    private data class QueryResponse(
        val type: String,
        val filter: String,
        val size: Int?,
    ): JavaSerializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
