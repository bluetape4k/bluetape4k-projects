package io.bluetape4k.geohash.benchmark

import io.bluetape4k.geohash.tests.RandomGeoHashes
import io.bluetape4k.logging.KLogging
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.NANOSECONDS)
class GeoHashEncodingBenchmark {

    companion object: KLogging()

    @Benchmark
    fun createGeoHashWith32() {
        RandomGeoHashes.createWithPrecision(32)
    }

    @Benchmark
    fun createGeoHashWith60() {
        RandomGeoHashes.createWithPrecision(60)
    }
}
