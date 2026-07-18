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
class AvroSerializerAllocationBenchmark {
    private lateinit var reflect: SerializerBenchmarkFixture
    private lateinit var compatibilityTarget: ByteBuffer
    private lateinit var optimizedTarget: ByteBuffer

    @Setup
    fun setup() {
        reflect = avroSerializerBenchmarkFixture().also { it.validate() }
        compatibilityTarget = reflect.newTarget()
        optimizedTarget = reflect.newTarget()
    }

    @Benchmark
    fun avroReflectSerializeByteArray(blackhole: Blackhole) {
        blackhole.consume(reflect.serializeByteArray())
    }

    @Benchmark
    fun avroReflectSerializeCompatibility(blackhole: Blackhole) {
        compatibilityTarget.clear()
        blackhole.consume(reflect.serializeCompatibility(compatibilityTarget))
    }

    @Benchmark
    fun avroReflectSerializeOptimized(blackhole: Blackhole) {
        optimizedTarget.clear()
        blackhole.consume(reflect.serializeOptimized(optimizedTarget))
    }

    @Benchmark
    fun avroReflectDeserializeByteArray(blackhole: Blackhole) {
        blackhole.consume(reflect.deserializeByteArray())
    }

    @Benchmark
    fun avroReflectDeserializeCompatibility(blackhole: Blackhole) {
        blackhole.consume(reflect.deserializeCompatibility(reflect.precomputedOptimizedSource()))
    }

    @Benchmark
    fun avroReflectDeserializeOptimized(blackhole: Blackhole) {
        blackhole.consume(reflect.deserializeOptimized(reflect.precomputedOptimizedSource()))
    }
}
