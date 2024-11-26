package io.bluetape4k.spring.tests

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Nested
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.Test

class WebTestExtensionsTest {

    companion object: KLogging()

    private val client = WebTestClient.bindToServer()
        .baseUrl("https://nghttp2.org/httpbin")
        .build()

    @Nested
    inner class Get {
        @Test
        fun `httGet httpbin`() {
            client.httpGet("/get")
                .expectBody()
                .jsonPath("$.url").isEqualTo("https://nghttp2.org/httpbin/get")
        }

        @Test
        fun `httGet httpbin anything`() {
            val response = client.httpGet("/anything")
                .expectBody<String>().returnResult().responseBody!!
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
                .jsonPath("$.url").isEqualTo("https://nghttp2.org/httpbin/post")
        }

        @Test
        fun `httpPost httpbin with body`() {
            client.httpPost("/post", "Hello, World!")
                .expectBody()
                .jsonPath("$.url").isEqualTo("https://nghttp2.org/httpbin/post")
        }
    }

    @Nested
    inner class Patch {
        @Test
        fun `httpPatch httpbin`() {
            client.httpPatch("/patch")
                .expectBody()
                .jsonPath("$.url").isEqualTo("https://nghttp2.org/httpbin/patch")
        }

        @Test
        fun `httpPatch httpbin with body`() {
            client.httpPatch("/patch", "Hello, World!")
                .expectBody()
                .jsonPath("$.url").isEqualTo("https://nghttp2.org/httpbin/patch")
        }
    }

    @Nested
    inner class Put {
        @Test
        fun `httpPut httpbin`() {
            client.httpPut("/put")
                .expectBody()
                .jsonPath("$.url").isEqualTo("https://nghttp2.org/httpbin/put")
        }

        @Test
        fun `httpPut httpbin with body`() {
            client.httpPut("/put", "Hello, World!")
                .expectBody()
                .jsonPath("$.url").isEqualTo("https://nghttp2.org/httpbin/put")
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `httpDelete httpbin`() {
            client.httpDelete("/delete")
                .expectBody()
                .jsonPath("$.url").isEqualTo("https://nghttp2.org/httpbin/delete")
        }
    }
}
