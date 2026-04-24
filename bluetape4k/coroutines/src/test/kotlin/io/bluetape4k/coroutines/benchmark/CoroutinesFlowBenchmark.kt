package io.bluetape4k.coroutines.benchmark

import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.coroutines.flow.extensions.chunked
import io.bluetape4k.coroutines.flow.extensions.concatMapEager
import io.bluetape4k.coroutines.flow.extensions.mapParallel
import io.bluetape4k.coroutines.flow.extensions.parallel.map
import io.bluetape4k.coroutines.flow.extensions.parallel.parallel
import io.bluetape4k.coroutines.flow.extensions.parallel.sequential
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import org.openjdk.jmh.annotations.Fork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * bluetape4k-coroutines Flow 연산자 throughput 벤치마크.
 *
 * 실행: ./gradlew :bluetape4k-coroutines:benchmark
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class CoroutinesFlowBenchmark {

    companion object {
        private const val ITEMS = 10_000
        private const val SMALL_ITEMS = 1_000
        private const val TINY_ITEMS = 100
    }

    private val concurrency: Int = Runtime.getRuntime().availableProcessors()

    @Benchmark
    fun asyncFlowMapThroughput(): Int = runBlocking {
        (1..ITEMS).asFlow()
            .async { it * 2 }
            .toList()
            .size
    }

    @Benchmark
    fun flatMapMergeThroughput(): Int = runBlocking {
        (1..SMALL_ITEMS).asFlow()
            .flatMapMerge(concurrency) { n ->
                flowOf(n * 2)
            }
            .toList()
            .size
    }

    @Benchmark
    fun chunkedFlowThroughput(): Int = runBlocking {
        (1..ITEMS).asFlow()
            .chunked(100)
            .toList()
            .sumOf { it.size }
    }

    @Benchmark
    fun parallelFlowMapThroughput(): Int = runBlocking {
        (1..ITEMS).asFlow()
            .parallel(concurrency) { Dispatchers.Default }
            .map { it * 2 }
            .sequential()
            .toList()
            .size
    }

    @Benchmark
    fun concatMapEagerThroughput(): Int = runBlocking {
        (1..TINY_ITEMS).asFlow()
            .concatMapEager { n ->
                flow { emit(n * 2) }
            }
            .toList()
            .size
    }

    @Benchmark
    fun mapParallelThroughput(): Int = runBlocking {
        (1..SMALL_ITEMS).asFlow()
            .mapParallel(concurrency) { it * 2 }
            .toList()
            .size
    }

    @Benchmark
    fun plainMapBaselineThroughput(): Int = runBlocking {
        (1..ITEMS).asFlow()
            .map { it * 2 }
            .toList()
            .size
    }
}
