package io.bluetape4k.jackson3.async

import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.jackson3.writeAsString
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.testcontainers.http.HttpbinHttp2Server
import kotlinx.coroutines.reactive.asFlow
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.web.reactive.function.client.WebClient

class WebClientStreamingExampleTest {

    private val mapper = Jackson.defaultJsonMapper
    private val httpbinServer by lazy { HttpbinHttp2Server.Launcher.httpbinHttp2 }
    private val client: WebClient by lazy {
        WebClient.builder()
            .baseUrl(httpbinServer.url)
            .build()
    }

    @Test
    fun `WebClient stream response can be parsed by async parser`() {
        val roots = mutableListOf<String>()
        val parser = AsyncJsonParser { root ->
            roots += root.get("url").asText()
        }

        client.get()
            .uri("/stream/3")
            .retrieve()
            .bodyToFlux(DataBuffer::class.java)
            .doOnNext { buffer -> parser.consume(buffer.toByteArrayAndRelease()) }
            .blockLast()

        roots.size shouldBeEqualTo 3
        roots.forEach { it shouldContain "/stream/3" }
    }

    @Test
    fun `WebClient stream response can be parsed by suspend parser`() = runSuspendIO {
        val roots = mutableListOf<String>()
        val parser = SuspendJsonParser { root ->
            roots += mapper.writeAsString(root).orEmpty()
        }

        val byteFlow = client.get()
            .uri("/stream/3")
            .retrieve()
            .bodyToFlux(DataBuffer::class.java)
            .map { buffer -> buffer.toByteArrayAndRelease() }
            .asFlow()

        parser.consume(byteFlow)

        roots.size shouldBeEqualTo 3
        roots.forEach { it shouldContain "/stream/3" }
    }

    private fun DataBuffer.toByteArrayAndRelease(): ByteArray =
        try {
            ByteArray(readableByteCount()).also { read(it) }
        } finally {
            DataBufferUtils.release(this)
        }
}
