package io.bluetape4k.spring.tests

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import kotlin.test.Test

class WebTestClientExtensionsTest: AbstractSpringTest() {

    companion object: KLoggingChannel()

    private val client: WebTestClient = WebTestClient
        .bindToServer()
        .baseUrl(baseUrl)
        .build()

    @Nested
    inner class Get {
        @Test
        fun `httGet httpbin`() {
            client.httpGet("/get")
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/get")
        }

        @Test
        fun `httGet httpbin anything`() = runTest {
            client.httpGet("/anything")
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/anything")

            val response = client.httpGet("/anything")
                .returnResult<String>().responseBody
                .asFlow()
                .toList()
                .joinToString(separator = "")

            log.debug { "anything response=$response" }
        }

        @Test
        fun `httGet httpbin not found`() {
            client.httpGet("/not-existing", HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class Post {
        @Test
        fun `httpPost httpbin`() {
            client.httpPost("/post")
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/post")
        }

        @Test
        fun `httpPost httpbin with body`() {
            client.httpPost("/post", "Hello, World!")
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/post")
        }

        @Test
        fun `httpPost httpbin with flow`() {
            client.httpPost("/post", flowOf("Hello", ",", "World!"))
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/post")
        }
    }

    @Nested
    inner class Patch {
        @Test
        fun `httpPatch httpbin`() {
            client.httpPatch("/patch")
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/patch")
        }

        @Test
        fun `httpPatch httpbin with body`() {
            client.httpPatch("/patch", "Hello, World!")
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/patch")
        }
    }

    @Nested
    inner class Put {
        @Test
        fun `httpPut httpbin`() {
            client.httpPut("/put")
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/put")
        }

        @Test
        fun `httpPut httpbin with body`() {
            client.httpPut("/put", "Hello, World!")
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/put")
        }

        @Test
        fun `httpPut httpbin with flow`() {
            client.httpPut("/put", flowOf("Hello", ",", "World!"))
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/put")
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `httpDelete httpbin`() {
            client.httpDelete("/delete")
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/delete")
        }
    }
}
