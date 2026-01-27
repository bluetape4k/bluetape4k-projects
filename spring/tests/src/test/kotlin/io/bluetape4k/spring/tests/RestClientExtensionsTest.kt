package io.bluetape4k.spring.tests

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
            val response = client.httpGet("/get")
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/get"
        }

        @Test
        fun `httGet httpbin anything`() {
            val response = client.httpGet("/anything")
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/anything"
        }

        @Test
        fun `httGet httpbin not found`() {
            assertFailsWith<HttpClientErrorException.NotFound> {
                client.httpGet("/not-existing").toEntity<String>()
            }
        }
    }

    @Nested
    inner class Post {
        @Test
        fun `httpPost httpbin`() {
            val response = client.httpPost("/post")
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/post"
        }

        @Test
        fun `httpPost httpbin with body`() {
            val response = client.httpPost("/post", "Hello, World!")
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/post"
            response shouldContain "Hello, World!"
        }

        @Test
        fun `httpPost httpbin with flow`() {
            val response = client.httpPost("/post", flowOf("Hello", ",", "World!"))
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/post"
        }
    }

    @Nested
    inner class Patch {
        @Test
        fun `httpPatch httpbin`() {
            val response = client.httpPatch("/patch")
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/patch"
        }

        @Test
        fun `httpPatch httpbin with body`() {
            val response = client.httpPatch("/patch", "Hello, World!")
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/patch"
            response shouldContain "Hello, World!"
        }

    }

    @Nested
    inner class Put {
        @Test
        fun `httpPut httpbin`() {
            val response = client.httpPut("/put")
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/put"
        }

        @Test
        fun `httpPut httpbin with body`() {
            val response = client.httpPut("/put", "Hello, World!")
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/put"
            response shouldContain "Hello, World!"
        }

        @Test
        fun `httpPut httpbin with flow`() {
            val response = client.httpPut("/put", flowOf("Hello", ",", "World!"))
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/put"
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `httpDelete httpbin`() {
            val response = client.httpDelete("/delete")
                .toEntity<String>()
                .body.shouldNotBeNull()

            log.debug { "response=$response" }
            response shouldContain "$baseUrl/delete"
        }
    }
}
