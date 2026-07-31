package io.bluetape4k.io.benchmark

import io.bluetape4k.io.compressor.Compressor
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

internal object CallerOwnedCompressionDispatch {
    fun source(storagePath: String, bytes: ByteArray): ByteBuffer = when (storagePath) {
        "direct", "directToHeap" -> ByteBuffer.allocateDirect(bytes.size).put(bytes).flip()
        "heap", "heapToDirect" -> ByteBuffer.wrap(bytes)
        else -> error("Unknown storagePath=$storagePath")
    }

    fun target(storagePath: String, capacity: Int): ByteBuffer = when (storagePath) {
        "direct", "heapToDirect" -> ByteBuffer.allocateDirect(capacity)
        "heap", "directToHeap" -> ByteBuffer.allocate(capacity)
        else -> error("Unknown storagePath=$storagePath")
    }

    fun eligible(codec: String, operation: String, storagePath: String): Boolean = when (codec) {
        "lz4", "deflate" -> storagePath in setOf("heap", "direct", "heapToDirect", "directToHeap")
        "snappy" -> operation == "compress" && storagePath == "direct"
        "zstd" -> storagePath in setOf("heap", "direct")
        else -> false
    }
}

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class CallerOwnedByteBufferCompressorBenchmark {
    @Param("lz4", "deflate", "snappy", "zstd")
    lateinit var compressorName: String

    @Param("small", "medium", "large")
    lateinit var payloadSize: String

    @Param("heap", "direct", "heapToDirect", "directToHeap")
    lateinit var storagePath: String

    private lateinit var compressor: Compressor
    private lateinit var payload: ByteArray
    private lateinit var wire: ByteArray
    private lateinit var plainSource: ByteBuffer
    private lateinit var compressedSource: ByteBuffer
    private lateinit var compressedTarget: ByteBuffer
    private lateinit var restoredTarget: ByteBuffer
    private var plainStart = 0
    private var plainLimit = 0
    private var compressedStart = 0
    private var compressedLimit = 0
    private var compressedTargetStart = 0
    private var compressedTargetLimit = 0
    private var restoredTargetStart = 0
    private var restoredTargetLimit = 0

    @Setup(Level.Trial)
    fun setup() {
        compressor = SameConditionCompressionPayloads.commonCompressor(compressorName).compressor
        val size = SameConditionPayloadSize.valueOf(payloadSize.replaceFirstChar(Char::uppercaseChar))
        payload = SameConditionCompressionPayloads.payload(SameConditionPayloadKind.Json, size)
        wire = compressor.compress(payload)
        plainSource = CallerOwnedCompressionDispatch.source(storagePath, payload)
        compressedSource = CallerOwnedCompressionDispatch.source(storagePath, wire)
        compressedTarget = CallerOwnedCompressionDispatch.target(
            storagePath,
            maxOf(payload.size * 2 + 64, wire.size * 2 + 64),
        )
        restoredTarget = CallerOwnedCompressionDispatch.target(storagePath, payload.size)
        plainStart = plainSource.position()
        plainLimit = plainSource.limit()
        compressedStart = compressedSource.position()
        compressedLimit = compressedSource.limit()
        compressedTargetStart = compressedTarget.position()
        compressedTargetLimit = compressedTarget.limit()
        restoredTargetStart = restoredTarget.position()
        restoredTargetLimit = restoredTarget.limit()
        validateRoundTrip()
    }

    fun validateRoundTrip() {
        plainSource.limit(plainLimit).position(plainStart)
        compressedTarget.limit(compressedTargetLimit).position(compressedTargetStart)
        val compressedSize = compressor.compress(plainSource, compressedTarget)
        val candidateWire = ByteArray(compressedSize).also { bytes ->
            compressedTarget.duplicate()
                .position(compressedTargetStart)
                .limit(compressedTargetStart + compressedSize)
                .get(bytes)
        }
        check(compressor.decompress(candidateWire).contentEquals(payload))

        resetCallerOwned()
        val written = compressor.decompress(compressedSource, restoredTarget)
        check(written == payload.size)
        val restored = ByteArray(written).also { bytes ->
            restoredTarget.duplicate()
                .position(restoredTargetStart)
                .limit(restoredTargetStart + written)
                .get(bytes)
        }
        check(restored.contentEquals(payload))
    }

    @Benchmark
    fun compressByteArrayBaseline(): ByteArray = compressor.compress(payload)

    @Benchmark
    fun compressByteBufferBaseline(): ByteBuffer {
        plainSource.limit(plainLimit).position(plainStart)
        return compressor.compress(plainSource)
    }

    @Benchmark
    fun compressCallerOwned(): Int {
        plainSource.limit(plainLimit).position(plainStart)
        compressedTarget.limit(compressedTargetLimit).position(compressedTargetStart)
        return compressor.compress(plainSource, compressedTarget)
    }

    @Benchmark
    fun decompressByteArrayBaseline(): ByteArray = compressor.decompress(wire)

    @Benchmark
    fun decompressByteBufferBaseline(): ByteBuffer {
        compressedSource.limit(compressedLimit).position(compressedStart)
        return compressor.decompress(compressedSource)
    }

    @Benchmark
    fun decompressCallerOwned(): Int {
        resetCallerOwned()
        return compressor.decompress(compressedSource, restoredTarget)
    }

    private fun resetCallerOwned() {
        compressedSource.limit(compressedLimit).position(compressedStart)
        restoredTarget.limit(restoredTargetLimit).position(restoredTargetStart)
    }
}
