package io.bluetape4k.testcontainers.infra

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class EtcdServerTest: AbstractContainerTest() {

    @Test
    fun `launch Etcd server`() {
        EtcdServer().use { etcd ->
            etcd.start()

            etcd.isRunning.shouldBeTrue()
            etcd.endpoint.startsWith("http://").shouldBeTrue()
            etcd.endpoints shouldBeEqualTo listOf(etcd.endpoint)
            etcd.properties()["endpoint"] shouldBeEqualTo etcd.endpoint
            System.getProperty("testcontainers.etcd.endpoint") shouldBeEqualTo etcd.endpoint

            val response = httpClient.send(
                HttpRequest.newBuilder(URI.create("${etcd.endpoint}/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            response.statusCode() shouldBeEqualTo 200
            response.body().contains("\"health\"").shouldBeTrue()
        }
    }

    @Test
    fun `blank image tag is rejected`() {
        assertFailsWith<IllegalArgumentException> { EtcdServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { EtcdServer(tag = " ") }
    }

    @Test
    fun `uses provided image`() {
        val server = EtcdServer(image = "gcr.io/etcd-development/etcd", tag = EtcdServer.TAG)
        server.dockerImageName shouldBeEqualTo "gcr.io/etcd-development/etcd:${EtcdServer.TAG}"
    }

    companion object {
        private val httpClient: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
    }
}
