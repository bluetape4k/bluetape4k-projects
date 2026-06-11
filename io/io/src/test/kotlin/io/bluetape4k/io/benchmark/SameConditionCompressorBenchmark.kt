package io.bluetape4k.io.benchmark

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.io.compressor.Compressor
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import org.openjdk.jmh.annotations.Param
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class SameConditionCompressorBenchmark {

    @Param("json", "text", "binary", "random")
    lateinit var payloadKind: String

    @Param("small", "medium", "large")
    lateinit var payloadSize: String

    @Param("gzip", "deflate", "zstd", "lz4", "snappy")
    lateinit var compressorName: String

    private lateinit var payloadBytes: ByteArray
    private lateinit var compressedBytes: ByteArray
    private lateinit var compressor: Compressor

    @Setup
    fun setup() {
        val kind = SameConditionPayloadKind.valueOf(payloadKind.replaceFirstChar(Char::uppercaseChar))
        val size = SameConditionPayloadSize.valueOf(payloadSize.replaceFirstChar(Char::uppercaseChar))
        val selected = SameConditionCompressionPayloads.commonCompressor(compressorName)

        payloadBytes = SameConditionCompressionPayloads.payload(kind, size)
        compressor = selected.compressor
        compressedBytes = compressor.compress(payloadBytes)
        compressor.decompress(compressedBytes) shouldBeEqualTo payloadBytes
    }

    @Benchmark
    fun compress(): ByteArray =
        compressor.compress(payloadBytes)

    @Benchmark
    fun decompress(): ByteArray =
        compressor.decompress(compressedBytes)
}
