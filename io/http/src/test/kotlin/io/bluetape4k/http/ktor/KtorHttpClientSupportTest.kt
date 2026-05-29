package io.bluetape4k.http.ktor

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.Serializable
import java.time.Duration
import kotlinx.serialization.Serializable as KotlinSerializable

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorHttpClientSupportTest : AbstractHttpTest() {

    companion object : KLogging()

    @Test
    fun `ktorCioHttpClientOf creates CIO-backed client`() {
        val client = ktorCioHttpClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `ktorCioHttpClientOf applies config block`() {
        val client = ktorCioHttpClientOf {
            engine {
                requestTimeout = 5_000
            }
        }
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `ktorCioHttpClientOf GET request returns 200`() = runSuspendIO {
        val client = ktorCioHttpClientOf()
        try {
            val response = client.get("$httpbinBaseUrl/get")
            response.status shouldBeEqualTo HttpStatusCode.OK
            response.bodyAsText().shouldNotBeBlank()
        } finally {
            client.close()
        }
    }

    @Test
    fun `ktorHttpClientOf with CIO engine GET request returns 200`() = runSuspendIO {
        val client = ktorHttpClientOf(io.ktor.client.engine.cio.CIO)
        try {
            val response = client.get("$httpbinBaseUrl/get")
            response.status shouldBeEqualTo HttpStatusCode.OK
        } finally {
            client.close()
        }
    }

    @Test
    fun `ktorJsonHttpClientOf installs JSON serialization with explicit engine`() = runSuspendIO {
        val client = ktorJsonHttpClientOf(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = """{"status":"UP","ignored":"additive"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            }
        }

        try {
            val response = client.get("https://example.test/health")
            response.status shouldBeEqualTo HttpStatusCode.OK
            response.body<ClientHealthResponse>().status shouldBeEqualTo "UP"
        } finally {
            client.close()
        }
    }

    @Test
    fun `ktorCioJsonHttpClientOf creates CIO client with shared defaults`() {
        val client = ktorCioJsonHttpClientOf(
            timeouts = KtorClientTimeouts(
                requestTimeout = Duration.ofSeconds(5),
                connectTimeout = Duration.ofSeconds(2),
                socketTimeout = Duration.ofSeconds(5),
            )
        )

        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `KtorClientTimeouts reject non positive durations`() {
        assertFailsWith<IllegalArgumentException> {
            KtorClientTimeouts(requestTimeout = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            KtorClientTimeouts(connectTimeout = Duration.ofMillis(-1))
        }
        assertFailsWith<IllegalArgumentException> {
            KtorClientTimeouts(socketTimeout = Duration.ZERO)
        }
    }
}

@KotlinSerializable
internal data class ClientHealthResponse(
    val status: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
