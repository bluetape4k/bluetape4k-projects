package io.bluetape4k.redis.redisson.benchmark

import io.bluetape4k.io.serializer.ForyBinarySerializer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.io.OutputStream
import java.io.Serializable
import java.util.concurrent.TimeUnit

internal const val ISSUE756_FORY_INITIAL_CAPACITY = 256

internal val ISSUE756_FORY_PAYLOAD = Issue756ForyBenchmarkData(
    id = 756L,
    name = "lettuce-buffer-codec",
    description = "A".repeat(96),
)

/** Fixed payload for the non-promotable Redisson Fory encode feasibility probe. */
data class Issue756ForyBenchmarkData(
    val id: Long,
    val name: String,
    val description: String,
): Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private class ByteBufOutputStream(
    private val target: ByteBuf,
): OutputStream() {
    override fun write(value: Int) {
        target.writeByte(value)
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        target.writeBytes(bytes, offset, length)
    }

    override fun flush() = Unit

    override fun close() = Unit
}

/**
 * Compares the current returned-array handoff with a fresh caller-owned stream target.
 *
 * This probe is deliberately benchmark-local. It cannot justify a production encode change unless both independent
 * runs satisfy the allocation confidence-interval and throughput gates.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(1)
@Fork(value = 2, jvmArgs = ["-Xms1g", "-Xmx1g", "-XX:+UseG1GC"])
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class Issue756ForyEncodeFeasibilityBenchmark {
    private val fory = ForyBinarySerializer()
    private val fastFory = ForyBinarySerializer.fast()

    @Benchmark
    fun foryBaseline(blackhole: Blackhole) {
        encodeBaseline(fory, blackhole)
    }

    @Benchmark
    fun foryCandidate(blackhole: Blackhole) {
        encodeCandidate(fory, blackhole)
    }

    @Benchmark
    fun fastForyBaseline(blackhole: Blackhole) {
        encodeBaseline(fastFory, blackhole)
    }

    @Benchmark
    fun fastForyCandidate(blackhole: Blackhole) {
        encodeCandidate(fastFory, blackhole)
    }

    private fun encodeBaseline(serializer: ForyBinarySerializer, blackhole: Blackhole) {
        val buffer = Unpooled.wrappedBuffer(serializer.serialize(ISSUE756_FORY_PAYLOAD))
        try {
            blackhole.consume(buffer.readableBytes())
            blackhole.consume(buffer.getByte(buffer.readerIndex()))
        } finally {
            check(buffer.release()) { "Baseline buffer was not released exactly once." }
        }
    }

    private fun encodeCandidate(serializer: ForyBinarySerializer, blackhole: Blackhole) {
        val buffer = Unpooled.buffer(ISSUE756_FORY_INITIAL_CAPACITY, Int.MAX_VALUE)
        try {
            val reported = serializer.serializeBinaryToStream(ISSUE756_FORY_PAYLOAD, ByteBufOutputStream(buffer))
            check(reported == buffer.readableBytes()) { "Fory stream byte count differs from the written range." }
            blackhole.consume(reported)
            blackhole.consume(buffer.getByte(buffer.readerIndex()))
        } finally {
            check(buffer.release()) { "Candidate buffer was not released exactly once." }
        }
    }
}
