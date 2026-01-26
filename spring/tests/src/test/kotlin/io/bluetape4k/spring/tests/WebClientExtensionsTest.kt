package io.bluetape4k.spring.tests

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.workshop.shared.httpDelete
import io.bluetape4k.workshop.shared.httpGet
import io.bluetape4k.workshop.shared.httpPatch
import io.bluetape4k.workshop.shared.httpPost
import io.bluetape4k.workshop.shared.httpPut
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WebClientExtensionsTest: AbstractSpringTest() {

    companion object: KLoggingChannel()

    private val client: WebClient = WebClient
        .builder()
        .baseUrl(baseUrl)
        .build()

    @Nested
    inner class Get {
        @Test
        fun `httGet httpbin`() = runSuspendIO {
            val response = client.httpGet("/get")
                .awaitBody<String>()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/get"
        }

        @Test
        fun `httGet httpbin anything`() = runSuspendIO {
            val response = client.httpGet("/anything")
                .awaitBody<String>()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/anything"
        }

        @Test
        fun `httGet httpbin not found`() = runSuspendIO {
            assertFailsWith<WebClientResponseException.NotFound> {
                val response = client.httpGet("/not-existing")
                    .awaitBodyOrNull<String>()

                log.debug { "response=$response" }

            }
        }
    }

    @Nested
    inner class Post {
        @Test
        fun `httpPost httpbin`() = runSuspendIO {
            val response = client.httpPost("/post")
                .awaitBody<String>()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/post"
        }

        @Test
        fun `httpPost httpbin with body`() = runSuspendIO {
            val response = client.httpPost("/post", "Hello, World!")
                .awaitBody<String>()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/post"
            response shouldContain "Hello, World!"
        }

        @Test
        fun `httpPost httpbin with flow`() = runSuspendIO {
            val response = client.httpPost("/post", flowOf("Hello", ", ", "World!"))
                .awaitBody<String>()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/post"
            response shouldContain "Hello, World!"
        }
    }

    @Nested
    inner class Patch {
        @Test
        fun `httpPatch httpbin`() = runSuspendIO {
            val response = client
                .httpPatch("/patch")
                .awaitBody<String>()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/patch"
        }

        @Test
        fun `httpPatch httpbin with body`() = runSuspendIO {
            val response = client
                .httpPatch("/patch", "Hello, World!")
                .awaitBody<String>()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/patch"
            response shouldContain "Hello, World!"
        }
    }

    @Nested
    inner class Put {
        @Test
        fun `httpPut httpbin`() = runSuspendIO {
            val response = client.httpPut("/put")
                .awaitBody<String>()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/put"
        }

        @Test
        fun `httpPut httpbin with body`() = runSuspendIO {
            val response = client.httpPut("/put", "Hello, World!")
                .awaitBody<String>()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/put"
            response shouldContain "Hello, World!"
        }

        @Test
        fun `httpPut httpbin with flow`() = runSuspendIO {
            val response = client.httpPut("/put", flowOf("Hello", ", ", "World!"))
                .awaitBody<String>()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/put"
            response shouldContain "Hello, World!"
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `httpDelete httpbin`() = runSuspendIO {
            val response = client.httpDelete("/delete")
                .awaitBodyOrNull<String>()

            log.debug { "response=$response" }
            response.shouldNotBeNull() shouldContain "$baseUrl/delete"
        }
    }
}
