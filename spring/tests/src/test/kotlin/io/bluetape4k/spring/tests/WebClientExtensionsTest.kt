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
import org.junit.jupiter.api.Nested
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import kotlin.test.Test

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
            client.httpGet("/get")
                .awaitBody<String>()
            // .jsonPath("$.url").isEqualTo("$baseUrl/get")
        }

        @Test
        fun `httGet httpbin anything`() = runSuspendIO {
            client.httpGet("/anything")
                .awaitBody<String>()
            // .jsonPath("$.url").isEqualTo("$baseUrl/anything")

            val response = client.httpGet("/anything")
                .awaitBody<String>()

            log.debug { "anything response=$response" }
        }

        @Test
        fun `httGet httpbin not found`() = runSuspendIO {
            client.httpGet("/not-existing")

        }
    }

    @Nested
    inner class Post {
        @Test
        fun `httpPost httpbin`() = runSuspendIO {
            client.httpPost("/post")
                .awaitBody<String>()
            // .jsonPath("$.url").isEqualTo("$baseUrl/post")
        }

        @Test
        fun `httpPost httpbin with body`() = runSuspendIO {
            client.httpPost("/post", "Hello, World!")
                .awaitBody<String>()
        }

        @Test
        fun `httpPost httpbin with flow`() = runSuspendIO {
            client.httpPost("/post", flowOf("Hello", ",", "World!"))
                .awaitBody<String>()
        }
    }

    @Nested
    inner class Patch {
        @Test
        fun `httpPatch httpbin`() = runSuspendIO {
            client.httpPatch("/patch")
                .awaitBody<String>()
        }

        @Test
        fun `httpPatch httpbin with body`() = runSuspendIO {
            client.httpPatch("/patch", "Hello, World!")
                .awaitBody<String>()
        }
    }

    @Nested
    inner class Put {
        @Test
        fun `httpPut httpbin`() = runSuspendIO {
            client.httpPut("/put")
                .awaitBody<String>()
        }

        @Test
        fun `httpPut httpbin with body`() = runSuspendIO {
            client.httpPut("/put", "Hello, World!")
                .awaitBody<String>()
        }

        @Test
        fun `httpPut httpbin with flow`() = runSuspendIO {
            client.httpPut("/put", flowOf("Hello", ",", "World!"))
                .awaitBody<String>()
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `httpDelete httpbin`() = runSuspendIO {
            client.httpDelete("/delete")
                .awaitBodyOrNull<String>()
        }
    }
}
