package io.bluetape4k.benchmark.serializer

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import org.openjdk.jmh.infra.Blackhole
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
class JsonSerializerAllocationBenchmark {
    private lateinit var jackson2: SerializerBenchmarkFixture
    private lateinit var jackson3: SerializerBenchmarkFixture
    private lateinit var fastjson: SerializerBenchmarkFixture
    private lateinit var jackson2CompatibilityTarget: ByteBuffer
    private lateinit var jackson2OptimizedTarget: ByteBuffer
    private lateinit var jackson3CompatibilityTarget: ByteBuffer
    private lateinit var jackson3OptimizedTarget: ByteBuffer
    private lateinit var fastjsonFallbackTarget: ByteBuffer
    private lateinit var fastjsonFallbackDirectSource: ByteBuffer
    private lateinit var fastjsonFallbackReadOnlySource: ByteBuffer

    @Setup
    fun setup() {
        jackson2 = jsonSerializerBenchmarkFixture(JsonSerializerKind.JACKSON2).also { it.validate() }
        jackson3 = jsonSerializerBenchmarkFixture(JsonSerializerKind.JACKSON3).also { it.validate() }
        fastjson = jsonSerializerBenchmarkFixture(JsonSerializerKind.FASTJSON2).also { it.validate() }
        jackson2CompatibilityTarget = jackson2.newTarget()
        jackson2OptimizedTarget = jackson2.newTarget()
        jackson3CompatibilityTarget = jackson3.newTarget()
        jackson3OptimizedTarget = jackson3.newTarget()
        fastjsonFallbackTarget = fastjson.newTarget()
        fastjsonFallbackDirectSource = requireNotNull(fastjson.precomputedFallbackDirectSource())
        fastjsonFallbackReadOnlySource = requireNotNull(fastjson.precomputedFallbackReadOnlySource())
    }

    @Benchmark
    fun jackson2SerializeByteArray(blackhole: Blackhole) {
        blackhole.consume(jackson2.serializeByteArray())
    }

    @Benchmark
    fun jackson2SerializeCompatibility(blackhole: Blackhole) {
        jackson2CompatibilityTarget.clear()
        blackhole.consume(jackson2.serializeCompatibility(jackson2CompatibilityTarget))
    }

    @Benchmark
    fun jackson2SerializeOptimized(blackhole: Blackhole) {
        jackson2OptimizedTarget.clear()
        blackhole.consume(jackson2.serializeOptimized(jackson2OptimizedTarget))
    }

    @Benchmark
    fun jackson2DeserializeByteArray(blackhole: Blackhole) {
        blackhole.consume(jackson2.deserializeByteArray())
    }

    @Benchmark
    fun jackson2DeserializeCompatibility(blackhole: Blackhole) {
        blackhole.consume(jackson2.deserializeCompatibility(jackson2.precomputedOptimizedSource()))
    }

    @Benchmark
    fun jackson2DeserializeOptimized(blackhole: Blackhole) {
        blackhole.consume(jackson2.deserializeOptimized(jackson2.precomputedOptimizedSource()))
    }

    @Benchmark
    fun jackson3SerializeByteArray(blackhole: Blackhole) {
        blackhole.consume(jackson3.serializeByteArray())
    }

    @Benchmark
    fun jackson3SerializeCompatibility(blackhole: Blackhole) {
        jackson3CompatibilityTarget.clear()
        blackhole.consume(jackson3.serializeCompatibility(jackson3CompatibilityTarget))
    }

    @Benchmark
    fun jackson3SerializeOptimized(blackhole: Blackhole) {
        jackson3OptimizedTarget.clear()
        blackhole.consume(jackson3.serializeOptimized(jackson3OptimizedTarget))
    }

    @Benchmark
    fun jackson3DeserializeByteArray(blackhole: Blackhole) {
        blackhole.consume(jackson3.deserializeByteArray())
    }

    @Benchmark
    fun jackson3DeserializeCompatibility(blackhole: Blackhole) {
        blackhole.consume(jackson3.deserializeCompatibility(jackson3.precomputedOptimizedSource()))
    }

    @Benchmark
    fun jackson3DeserializeOptimized(blackhole: Blackhole) {
        blackhole.consume(jackson3.deserializeOptimized(jackson3.precomputedOptimizedSource()))
    }

    @Benchmark
    fun fastjsonSerializeByteArray(blackhole: Blackhole) {
        blackhole.consume(fastjson.serializeByteArray())
    }

    @Benchmark
    fun fastjsonSerializeFallback(blackhole: Blackhole) {
        fastjsonFallbackTarget.clear()
        blackhole.consume(fastjson.serializeOptimized(fastjsonFallbackTarget))
    }

    @Benchmark
    fun fastjsonDeserializeByteArray(blackhole: Blackhole) {
        blackhole.consume(fastjson.deserializeByteArray())
    }

    @Benchmark
    fun fastjsonDeserializeOptimizedHeap(blackhole: Blackhole) {
        blackhole.consume(fastjson.deserializeOptimized(fastjson.precomputedOptimizedSource()))
    }

    @Benchmark
    fun fastjsonDeserializeFallbackDirect(blackhole: Blackhole) {
        blackhole.consume(fastjson.deserializeOptimized(fastjsonFallbackDirectSource))
    }

    @Benchmark
    fun fastjsonDeserializeFallbackReadOnly(blackhole: Blackhole) {
        blackhole.consume(fastjson.deserializeOptimized(fastjsonFallbackReadOnlySource))
    }
}
