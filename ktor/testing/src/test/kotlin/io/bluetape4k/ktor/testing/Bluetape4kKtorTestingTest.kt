package io.bluetape4k.ktor.testing

import io.bluetape4k.assertions.shouldBeEqualTo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.Serializable as JavaSerializable

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Bluetape4kKtorTestingTest {

    @Test
    fun `core setup helper installs json status pages and routes`() = testApplication {
        installBluetape4kKtorCoreForTest {
            get("/echo") {
                call.respond(EchoResponse("blue"))
            }
            get("/bad") {
                throw IllegalArgumentException("Invalid input")
            }
        }

        val ok = client.get("/echo")
        ok shouldHaveStatus HttpStatusCode.OK
        ok.shouldHaveJsonBody(EchoResponse("blue"))

        client.get("/bad").shouldHaveApiError(
            ExpectedApiError(
                status = HttpStatusCode.BadRequest,
                error = "bad_request",
                message = "Invalid input",
                path = "/bad"
            )
        )
    }

    @Test
    fun `json client installs content negotiation with bluetape4k defaults`() = testApplication {
        installBluetape4kKtorCoreForTest {
            get("/echo") {
                call.respond(EchoResponse("client"))
            }
        }

        val jsonClient = bluetape4kJsonClient()
        val body = jsonClient.get("/echo").body<EchoResponse>()

        body shouldBeEqualTo EchoResponse("client")
    }

    @Test
    fun `json mock engine returns encoded body with content type`() {
        val client = HttpClient(
            bluetape4kJsonMockEngine(EchoResponse("mock"))
        )

        testApplication {
            client.use { mockClient ->
                val response = mockClient.get("/")
                response.status shouldBeEqualTo HttpStatusCode.OK
                response.headers["Content-Type"] shouldBeEqualTo "application/json"
                response.shouldHaveJsonBody(EchoResponse("mock"))
            }
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
}
