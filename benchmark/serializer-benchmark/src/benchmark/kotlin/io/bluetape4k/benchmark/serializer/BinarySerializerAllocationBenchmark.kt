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
class BinarySerializerAllocationBenchmark {
    private lateinit var jdk: SerializerBenchmarkFixture
    private lateinit var kryo: SerializerBenchmarkFixture
    private lateinit var fory: SerializerBenchmarkFixture
    private lateinit var jdkCompatibilityTarget: ByteBuffer
    private lateinit var jdkOptimizedTarget: ByteBuffer
    private lateinit var kryoCompatibilityTarget: ByteBuffer
    private lateinit var kryoOptimizedTarget: ByteBuffer
    private lateinit var foryFallbackTarget: ByteBuffer

    @Setup
    fun setup() {
        jdk = binarySerializerBenchmarkFixture(BinarySerializerKind.JDK).also { it.validate() }
        kryo = binarySerializerBenchmarkFixture(BinarySerializerKind.KRYO).also { it.validate() }
        fory = binarySerializerBenchmarkFixture(BinarySerializerKind.FORY).also { it.validate() }
        jdkCompatibilityTarget = jdk.newTarget()
        jdkOptimizedTarget = jdk.newTarget()
        kryoCompatibilityTarget = kryo.newTarget()
        kryoOptimizedTarget = kryo.newTarget()
        foryFallbackTarget = fory.newTarget()
    }

    @Benchmark
    fun jdkSerializeByteArray(blackhole: Blackhole) {
        blackhole.consume(jdk.serializeByteArray())
    }

    @Benchmark
    fun jdkSerializeCompatibility(blackhole: Blackhole) {
        jdkCompatibilityTarget.clear()
        blackhole.consume(jdk.serializeCompatibility(jdkCompatibilityTarget))
    }

    @Benchmark
    fun jdkSerializeOptimized(blackhole: Blackhole) {
        jdkOptimizedTarget.clear()
        blackhole.consume(jdk.serializeOptimized(jdkOptimizedTarget))
    }

    @Benchmark
    fun jdkDeserializeByteArray(blackhole: Blackhole) {
        blackhole.consume(jdk.deserializeByteArray())
    }

    @Benchmark
    fun jdkDeserializeCompatibility(blackhole: Blackhole) {
        blackhole.consume(jdk.deserializeCompatibility(jdk.precomputedOptimizedSource()))
    }

    @Benchmark
    fun jdkDeserializeOptimized(blackhole: Blackhole) {
        blackhole.consume(jdk.deserializeOptimized(jdk.precomputedOptimizedSource()))
    }

    @Benchmark
    fun kryoSerializeByteArray(blackhole: Blackhole) {
        blackhole.consume(kryo.serializeByteArray())
    }

    @Benchmark
    fun kryoSerializeCompatibility(blackhole: Blackhole) {
        kryoCompatibilityTarget.clear()
        blackhole.consume(kryo.serializeCompatibility(kryoCompatibilityTarget))
    }

    @Benchmark
    fun kryoSerializeOptimized(blackhole: Blackhole) {
        kryoOptimizedTarget.clear()
        blackhole.consume(kryo.serializeOptimized(kryoOptimizedTarget))
    }

    @Benchmark
    fun kryoDeserializeByteArray(blackhole: Blackhole) {
        blackhole.consume(kryo.deserializeByteArray())
    }

    @Benchmark
    fun kryoDeserializeCompatibility(blackhole: Blackhole) {
        blackhole.consume(kryo.deserializeCompatibility(kryo.precomputedOptimizedSource()))
    }

    @Benchmark
    fun kryoDeserializeOptimized(blackhole: Blackhole) {
        blackhole.consume(kryo.deserializeOptimized(kryo.precomputedOptimizedSource()))
    }

    @Benchmark
    fun forySerializeByteArray(blackhole: Blackhole) {
        blackhole.consume(fory.serializeByteArray())
    }

    @Benchmark
    fun forySerializeFallback(blackhole: Blackhole) {
        foryFallbackTarget.clear()
        blackhole.consume(fory.serializeOptimized(foryFallbackTarget))
    }

    @Benchmark
    fun foryDeserializeByteArray(blackhole: Blackhole) {
        blackhole.consume(fory.deserializeByteArray())
    }

    @Benchmark
    fun foryDeserializeOptimized(blackhole: Blackhole) {
        blackhole.consume(fory.deserializeOptimized(fory.precomputedOptimizedSource()))
    }
}
