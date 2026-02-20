package io.bluetape4k.spring.tests

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
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
            val response = client
                .httpGet("/get")
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/get")
                .returnResult()
                .responseBody?.toUtf8String()

            log.debug { "response=$response" }
            response.shouldNotBeNull() shouldContain "$baseUrl/get"
        }

        @Test
        fun `httGet httpbin anything`() = runTest {
            val response = client
                .httpGet("/anything")
                .expectStatus().is2xxSuccessful
                .expectBody<String>()
                .returnResult().responseBody
                .shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/anything"
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
            client
                .httpPost("/post")
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/post")
        }

        @Test
        fun `httpPost httpbin with body`() {
            client
                .httpPost("/post", "Hello, World!")
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/post")
                .jsonPath("$.data").isEqualTo("Hello, World!")
        }

        @Test
        fun `httpPost httpbin with flow`() {
            client
                .httpPost("/post", flowOf("Hello", ", ", "World!"))
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/post")
                .jsonPath("$.data").isEqualTo("Hello, World!")
        }
    }

    @Nested
    inner class Patch {
        @Test
        fun `httpPatch httpbin`() {
            client
                .httpPatch("/patch")
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/patch")
        }

        @Test
        fun `httpPatch httpbin with body`() {
            client
                .httpPatch("/patch", "Hello, World!")
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/patch")
                .jsonPath("$.data").isEqualTo("Hello, World!")
        }
    }

    @Nested
    inner class Put {
        @Test
        fun `httpPut httpbin`() {
            client
                .httpPut("/put")
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/put")
        }

        @Test
        fun `httpPut httpbin with body`() {
            client
                .httpPut("/put", "Hello, World!")
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/put")
                .jsonPath("$.data").isEqualTo("Hello, World!")
        }

        @Test
        fun `httpPut httpbin with flow`() {
            client
                .httpPut("/put", flowOf("Hello", ", ", "World!"))
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/put")
                .jsonPath("$.data").isEqualTo("Hello, World!")
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `httpDelete httpbin`() {
            client
                .httpDelete("/delete")
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("$.url").isEqualTo("$baseUrl/delete")
        }
    }

    @Nested
    inner class Head {
        @Test
        fun `httpHead httpbin`() {
            client
                .httpHead("/get")
                .expectStatus().is2xxSuccessful
        }
    }

    @Nested
    inner class Options {
        @Test
        fun `httpOptions httpbin`() {
            client
                .httpOptions("/anything")
                .expectStatus().is2xxSuccessful
        }
    }
}
