package io.bluetape4k.geohash.benchmark

import io.bluetape4k.geohash.geoHashOfString
import io.bluetape4k.logging.KLogging
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.NANOSECONDS)
class GeoHashDecodingBenchmark {

    companion object: KLogging() {
        private const val HASH_COUNT = 1000
        private val BASE32 = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'b', 'c', 'd', 'e', 'f', 'g',
            'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
        )
    }

    private lateinit var randomHashes: Array<String>

    @Setup
    fun setup() {
        randomHashes = Array(HASH_COUNT) { "" }
        repeat(HASH_COUNT) {
            val chars = Random.nextInt(10) + 2
            randomHashes[it] = buildString {
                repeat(chars) {
                    append(BASE32[Random.nextInt(BASE32.size)])
                }
            }
        }
    }

    @Benchmark
    fun decodeFromBase32() {
        randomHashes.forEach {
            val geoHash = geoHashOfString(it)
        }
    }
}
