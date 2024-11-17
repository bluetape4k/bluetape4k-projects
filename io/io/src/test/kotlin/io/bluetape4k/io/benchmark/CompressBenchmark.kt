package io.bluetape4k.io.benchmark

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.utils.Resourcex
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import org.amshove.kluent.shouldBeEqualTo
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.NANOSECONDS)
class CompressBenchmark {

    private val gzip = Compressors.GZip
    private val deflate = Compressors.Deflate
    private val lz4 = Compressors.LZ4
    private val snappy = Compressors.Snappy
    private val zstd = Compressors.Zstd

    private val randomString = Resourcex.getString("files/Utf8Samples.txt")
    private val randomBytes = randomString.toUtf8Bytes()

    @Benchmark
    fun gzip() {
        with(gzip) {
            val compressed = compress(randomBytes)
            val decompressed = decompress(compressed)
            decompressed.toUtf8String() shouldBeEqualTo randomString
        }
    }

    @Benchmark
    fun deflate() {
        with(deflate) {
            val compressed = compress(randomBytes)
            val decompressed = decompress(compressed)
            decompressed.toUtf8String() shouldBeEqualTo randomString
        }
    }

    @Benchmark
    fun lz4() {
        with(lz4) {
            val compressed = compress(randomBytes)
            val decompressed = decompress(compressed)
            decompressed.toUtf8String() shouldBeEqualTo randomString
        }
    }

    @Benchmark
    fun snappy() {
        with(snappy) {
            val compressed = compress(randomBytes)
            val decompressed = decompress(compressed)
            decompressed.toUtf8String() shouldBeEqualTo randomString
        }
    }

    @Benchmark
    fun zstd() {
        with(zstd) {
            val compressed = compress(randomBytes)
            val decompressed = decompress(compressed)
            decompressed.toUtf8String() shouldBeEqualTo randomString
        }
    }
}
