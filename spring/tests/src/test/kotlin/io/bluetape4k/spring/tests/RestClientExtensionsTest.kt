package io.bluetape4k.spring.tests

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.workshop.shared.httpDelete
import io.bluetape4k.workshop.shared.httpGet
import io.bluetape4k.workshop.shared.httpPatch
import io.bluetape4k.workshop.shared.httpPost
import io.bluetape4k.workshop.shared.httpPut
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Nested
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import kotlin.test.Test

class RestClientExtensionsTest: AbstractSpringTest() {

    companion object: KLoggingChannel()

    private val client: RestClient = RestClient
        .builder()
        .baseUrl(baseUrl)
        .build()

    @Nested
    inner class Get {
        @Test
        fun `httGet httpbin`() {
            client.httpGet("/get")
                .toEntity<String>()
            // .jsonPath("$.url").isEqualTo("$baseUrl/get")
        }

        @Test
        fun `httGet httpbin anything`() {
            client.httpGet("/anything")
                .toEntity<String>()
            // .jsonPath("$.url").isEqualTo("$baseUrl/anything")

            val response = client.httpGet("/anything")
                .toEntity<String>()

            log.debug { "anything response=$response" }
        }

        @Test
        fun `httGet httpbin not found`() {
            client.httpGet("/not-existing")

        }
    }

    @Nested
    inner class Post {
        @Test
        fun `httpPost httpbin`() {
            client.httpPost("/post")
                .toEntity<String>()
            // .jsonPath("$.url").isEqualTo("$baseUrl/post")
        }

        @Test
        fun `httpPost httpbin with body`() {
            client.httpPost("/post", "Hello, World!")
                .toEntity<String>()
        }

        @Test
        fun `httpPost httpbin with flow`() {
            client.httpPost("/post", flowOf("Hello", ",", "World!"))
                .toEntity<String>()
        }
    }

    @Nested
    inner class Patch {
        @Test
        fun `httpPatch httpbin`() {
            client.httpPatch("/patch")
                .toEntity<String>()
        }

        @Test
        fun `httpPatch httpbin with body`() {
            client.httpPatch("/patch", "Hello, World!")
                .toEntity<String>()
        }
    }

    @Nested
    inner class Put {
        @Test
        fun `httpPut httpbin`() {
            client.httpPut("/put")
                .toEntity<String>()
        }

        @Test
        fun `httpPut httpbin with body`() {
            client.httpPut("/put", "Hello, World!")
                .toEntity<String>()
        }

        @Test
        fun `httpPut httpbin with flow`() {
            client.httpPut("/put", flowOf("Hello", ",", "World!"))
                .toEntity<String>()
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `httpDelete httpbin`() {
            client.httpDelete("/delete")
                .toEntity<String>()
        }
    }
}
