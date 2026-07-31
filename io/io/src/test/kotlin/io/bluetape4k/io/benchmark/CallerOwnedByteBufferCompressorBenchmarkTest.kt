package io.bluetape4k.io.benchmark

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.nio.file.Path
import kotlin.io.path.readText

class CallerOwnedByteBufferCompressorBenchmarkTest {

    @Test
    fun `mutable benchmark state is thread scoped`() {
        val annotation = CallerOwnedByteBufferCompressorBenchmark::class.java.getAnnotation(State::class.java)

        annotation.value shouldBeEqualTo Scope.Thread
    }

    @Test
    fun `eligibility matches backend storage capabilities`() {
        CallerOwnedCompressionDispatch.eligible("lz4", "compress", "heapToDirect") shouldBeEqualTo true
        CallerOwnedCompressionDispatch.eligible("deflate", "decompress", "directToHeap") shouldBeEqualTo true
        CallerOwnedCompressionDispatch.eligible("snappy", "compress", "direct") shouldBeEqualTo true
        CallerOwnedCompressionDispatch.eligible("snappy", "decompress", "direct") shouldBeEqualTo false
        CallerOwnedCompressionDispatch.eligible("snappy", "compress", "heap") shouldBeEqualTo false
        CallerOwnedCompressionDispatch.eligible("zstd", "compress", "directToHeap") shouldBeEqualTo false
    }

    @Test
    fun `dispatch creates the requested source and target storage`() {
        CallerOwnedCompressionDispatch.source("heap", byteArrayOf(1)).isDirect shouldBeEqualTo false
        CallerOwnedCompressionDispatch.source("direct", byteArrayOf(1)).isDirect shouldBeEqualTo true
        CallerOwnedCompressionDispatch.target("heapToDirect", 1).isDirect shouldBeEqualTo true
        CallerOwnedCompressionDispatch.target("directToHeap", 1).isDirect shouldBeEqualTo false
    }

    @Test
    fun `setup validates a caller-owned round trip`() {
        CallerOwnedByteBufferCompressorBenchmark().apply {
            compressorName = "zstd"
            payloadSize = "small"
            storagePath = "direct"
            setup()
            validateRoundTrip()
        }
    }

    @Test
    fun `evidence payload byte mapping matches the deterministic generator`() {
        val actual = SameConditionPayloadSize.entries.associate { size ->
            size.name.lowercase() to SameConditionCompressionPayloads
                .payload(SameConditionPayloadKind.Json, size)
                .size
        }

        actual shouldBeEqualTo mapOf("small" to 1147, "medium" to 65718, "large" to 524349)
    }

    @Test
    fun `measured methods do not allocate or copy payloads and restore limits`() {
        val source = Path.of("src/test/kotlin/io/bluetape4k/io/benchmark/CallerOwnedByteBufferCompressorBenchmark.kt")
            .readText()
        val methods = listOf(
            "compressByteArrayBaseline",
            "compressByteBufferBaseline",
            "compressCallerOwned",
            "decompressByteArrayBaseline",
            "decompressByteBufferBaseline",
            "decompressCallerOwned",
        )

        methods.forEach { method ->
            val body = measuredBody(source, method)
            listOf("allocate(", "allocateDirect(", "wrap(", "ByteArray(", ".copy", ".slice(").forEach { token ->
                body.contains(token) shouldBeEqualTo false
            }
        }
        measuredBody(source, "compressByteBufferBaseline").contains("limit(plainLimit)") shouldBeEqualTo true
        measuredBody(source, "compressCallerOwned").contains("limit(compressedTargetLimit)") shouldBeEqualTo true
        measuredBody(source, "decompressByteBufferBaseline").contains("limit(compressedLimit)") shouldBeEqualTo true
        measuredBody(source, "decompressCallerOwned").contains("resetCallerOwned()") shouldBeEqualTo true
    }

    private fun measuredBody(source: String, method: String): String {
        val start = source.indexOf("fun $method")
        check(start >= 0) { "missing benchmark method: $method" }
        val next = source.indexOf("\n    @Benchmark", start + 1).let { if (it < 0) source.length else it }
        return source.substring(start, next)
    }
}
