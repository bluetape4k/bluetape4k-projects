package io.bluetape4k.http.ktor

import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotBeBlank
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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
}
