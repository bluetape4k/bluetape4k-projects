package io.bluetape4k.benchmark.serializer

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.infra.Blackhole
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/** Measures codec-only Kafka allocation without broker, network, header, or buffer-construction noise. */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
class KafkaCodecAllocationBenchmark {
    private lateinit var fixture: KafkaCodecBenchmarkFixture
    private lateinit var target: ByteBuffer

    /** Builds and validates reusable fixture state outside timed benchmark cells. */
    @Setup(Level.Trial)
    fun setup() {
        fixture = KafkaCodecBenchmarkFixture().also { it.validate() }
        target = fixture.newTarget()
    }

    /** Resets the reusable output target before each timed invocation. */
    @Setup(Level.Invocation)
    fun resetTarget() {
        target.clear()
    }

    /** Measures Kafka codec serialization through the standard ByteArray path. */
    @Benchmark
    fun kafkaSerializeByteArray(blackhole: Blackhole) {
        blackhole.consume(fixture.serializeByteArray())
    }

    /** Measures Kafka codec serialization into the reusable caller-owned target. */
    @Benchmark
    fun kafkaSerializeOptimized(blackhole: Blackhole) {
        blackhole.consume(fixture.serializeOptimized(target))
    }

    /** Measures Kafka codec deserialization through the standard ByteArray path. */
    @Benchmark
    fun kafkaDeserializeByteArray(blackhole: Blackhole) {
        blackhole.consume(fixture.deserializeByteArray())
    }

    /** Measures Kafka codec deserialization from the reused read-only source. */
    @Benchmark
    fun kafkaDeserializeOptimized(blackhole: Blackhole) {
        blackhole.consume(fixture.deserializeOptimized())
    }
}
