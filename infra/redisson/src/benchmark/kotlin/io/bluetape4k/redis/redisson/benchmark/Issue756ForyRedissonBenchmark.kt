package io.bluetape4k.redis.redisson.benchmark

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.redis.redisson.codec.FastForyCodec
import io.bluetape4k.redis.redisson.codec.ForyCodec
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.CompositeByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import org.redisson.client.codec.Codec
import java.io.Serializable
import java.util.concurrent.TimeUnit

internal val ISSUE756_REDISSON_PAYLOAD = Issue756RedissonData(
    id = 756L,
    name = "lettuce-buffer-codec",
    description = "A".repeat(96),
)

data class Issue756RedissonData(
    val id: Long,
    val name: String,
    val description: String,
): Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

enum class Issue756RedissonSourceKind {
    HEAP,
    DIRECT,
    COMPOSITE,
}

abstract class Issue756RedissonDecodeState(
    private val sourceKind: Issue756RedissonSourceKind,
    private val serializer: BinarySerializer,
    private val codec: Codec,
) {
    private lateinit var source: ByteBuf

    @Setup(Level.Trial)
    fun setup() {
        val wire = serializer.serialize(ISSUE756_REDISSON_PAYLOAD)
        source = sourceBuffer(sourceKind, wire)
        check(source.readerIndex() == PREFIX_SIZE)
        check(source.readableBytes() == wire.size)
        check((source.nioBufferCount() == 1) == (sourceKind != Issue756RedissonSourceKind.COMPOSITE))
        check(codec.valueDecoder.decode(source, null) == ISSUE756_REDISSON_PAYLOAD)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        if (::source.isInitialized && source.refCnt() > 0) {
            source.release()
        }
    }

    fun copiedBaseline(blackhole: Blackhole) {
        val readerIndex = source.readerIndex()
        val refCnt = source.refCnt()
        val bytes = ByteBufUtil.getBytes(source, readerIndex, source.readableBytes(), true)
        blackhole.consume(serializer.deserialize<Any>(bytes))
        verifyState(readerIndex, refCnt)
    }

    fun candidate(blackhole: Blackhole) {
        val readerIndex = source.readerIndex()
        val refCnt = source.refCnt()
        blackhole.consume(codec.valueDecoder.decode(source, null))
        verifyState(readerIndex, refCnt)
    }

    private fun verifyState(readerIndex: Int, refCnt: Int) {
        check(source.readerIndex() == readerIndex)
        check(source.refCnt() == refCnt)
        check(source.getByte(0) == PREFIX)
    }

    private companion object {
        private const val PREFIX_SIZE = 3
        private const val PREFIX: Byte = 0x5A

        fun sourceBuffer(kind: Issue756RedissonSourceKind, wire: ByteArray): ByteBuf =
            when (kind) {
                Issue756RedissonSourceKind.HEAP -> PooledByteBufAllocator.DEFAULT.heapBuffer(PREFIX_SIZE + wire.size)
                    .writeZero(PREFIX_SIZE)
                    .setByte(0, PREFIX.toInt())
                    .writeBytes(wire)
                    .readerIndex(PREFIX_SIZE)

                Issue756RedissonSourceKind.DIRECT -> PooledByteBufAllocator.DEFAULT.directBuffer(PREFIX_SIZE + wire.size)
                    .writeZero(PREFIX_SIZE)
                    .setByte(0, PREFIX.toInt())
                    .writeBytes(wire)
                    .readerIndex(PREFIX_SIZE)

                Issue756RedissonSourceKind.COMPOSITE -> compositeSource(wire)
            }

        fun compositeSource(wire: ByteArray): CompositeByteBuf {
            val split = wire.size / 2
            val first = Unpooled.buffer(PREFIX_SIZE + split)
                .writeZero(PREFIX_SIZE)
                .setByte(0, PREFIX.toInt())
                .writeBytes(wire, 0, split)
            val second = Unpooled.wrappedBuffer(wire, split, wire.size - split)
            return Unpooled.compositeBuffer(2)
                .addComponents(true, first, second)
                .readerIndex(PREFIX_SIZE)
        }
    }
}

@State(Scope.Thread)
class ForyHeapDecodeState: Issue756RedissonDecodeState(
    Issue756RedissonSourceKind.HEAP,
    BinarySerializers.Fory,
    ForyCodec(),
)

@State(Scope.Thread)
class ForyDirectDecodeState: Issue756RedissonDecodeState(
    Issue756RedissonSourceKind.DIRECT,
    BinarySerializers.Fory,
    ForyCodec(),
)

@State(Scope.Thread)
class ForyCompositeDecodeState: Issue756RedissonDecodeState(
    Issue756RedissonSourceKind.COMPOSITE,
    BinarySerializers.Fory,
    ForyCodec(),
)

@State(Scope.Thread)
class FastForyHeapDecodeState: Issue756RedissonDecodeState(
    Issue756RedissonSourceKind.HEAP,
    BinarySerializers.FastFory,
    FastForyCodec(),
)

@State(Scope.Thread)
class FastForyDirectDecodeState: Issue756RedissonDecodeState(
    Issue756RedissonSourceKind.DIRECT,
    BinarySerializers.FastFory,
    FastForyCodec(),
)

@State(Scope.Thread)
class FastForyCompositeDecodeState: Issue756RedissonDecodeState(
    Issue756RedissonSourceKind.COMPOSITE,
    BinarySerializers.FastFory,
    FastForyCodec(),
)

/**
 * Issue #756 Redisson Fory decode allocation matrix.
 *
 * Composite cells document the copied fallback path and are non-promotable regardless of measured allocation.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2, jvmArgs = ["-Xms1g", "-Xmx1g", "-XX:+UseG1GC"])
@Threads(1)
open class Issue756ForyRedissonBenchmark {

    @Benchmark fun foryHeapDecodeCopiedBaseline(state: ForyHeapDecodeState, blackhole: Blackhole) =
        state.copiedBaseline(blackhole)
    @Benchmark fun foryHeapDecodeCandidate(state: ForyHeapDecodeState, blackhole: Blackhole) =
        state.candidate(blackhole)
    @Benchmark fun foryDirectDecodeCopiedBaseline(state: ForyDirectDecodeState, blackhole: Blackhole) =
        state.copiedBaseline(blackhole)
    @Benchmark fun foryDirectDecodeCandidate(state: ForyDirectDecodeState, blackhole: Blackhole) =
        state.candidate(blackhole)
    @Benchmark fun foryCompositeDecodeCopiedBaseline(state: ForyCompositeDecodeState, blackhole: Blackhole) =
        state.copiedBaseline(blackhole)
    @Benchmark fun foryCompositeDecodeCandidate(state: ForyCompositeDecodeState, blackhole: Blackhole) =
        state.candidate(blackhole)
    @Benchmark fun fastForyHeapDecodeCopiedBaseline(state: FastForyHeapDecodeState, blackhole: Blackhole) =
        state.copiedBaseline(blackhole)
    @Benchmark fun fastForyHeapDecodeCandidate(state: FastForyHeapDecodeState, blackhole: Blackhole) =
        state.candidate(blackhole)
    @Benchmark fun fastForyDirectDecodeCopiedBaseline(state: FastForyDirectDecodeState, blackhole: Blackhole) =
        state.copiedBaseline(blackhole)
    @Benchmark fun fastForyDirectDecodeCandidate(state: FastForyDirectDecodeState, blackhole: Blackhole) =
        state.candidate(blackhole)
    @Benchmark fun fastForyCompositeDecodeCopiedBaseline(state: FastForyCompositeDecodeState, blackhole: Blackhole) =
        state.copiedBaseline(blackhole)
    @Benchmark fun fastForyCompositeDecodeCandidate(state: FastForyCompositeDecodeState, blackhole: Blackhole) =
        state.candidate(blackhole)
}
