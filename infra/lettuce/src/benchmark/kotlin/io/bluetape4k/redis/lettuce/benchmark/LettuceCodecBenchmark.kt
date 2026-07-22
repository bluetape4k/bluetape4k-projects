package io.bluetape4k.redis.lettuce.benchmark

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.JdkBinarySerializer
import io.bluetape4k.io.serializer.KryoBinarySerializer
import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceJsonCodec
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
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
import java.io.Serializable
import java.util.concurrent.TimeUnit

internal const val ISSUE756_READER_INDEX = 3
internal const val ISSUE756_START_INDEX = 7
internal const val ISSUE756_PREFIX_INDEX = ISSUE756_START_INDEX - 1
internal const val ISSUE756_TARGET_CAPACITY = 512
internal const val ISSUE756_PREFIX: Byte = 0x5A
internal const val ISSUE756_SENTINEL: Byte = 0x33

internal val ISSUE756_PAYLOAD = Issue756BenchmarkData(
    id = 756L,
    name = "lettuce-buffer-codec",
    description = "A".repeat(96),
)

/** Deterministic payload shared by every issue #756 benchmark cell. */
data class Issue756BenchmarkData(
    val id: Long,
    val name: String,
    val description: String,
): Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

enum class Issue756TargetKind {
    HEAP,
    DIRECT,
}

/**
 * Shared caller-owned target lifecycle for the exact issue #756 promotion matrix.
 *
 * Value construction, target allocation, and target reset stay outside timed methods. Both paths start at the same
 * non-zero writer index in a fixed-capacity, pooled target, and both verify that no capacity growth occurred.
 */
abstract class Issue756TargetState(
    private val targetKind: Issue756TargetKind,
) {
    protected lateinit var target: ByteBuf
        private set

    private lateinit var expectedWire: ByteArray
    private var initialCapacity: Int = -1
    private var initialMaxCapacity: Int = -1

    protected abstract fun serializeCopiedBaseline(value: Issue756BenchmarkData): ByteArray

    protected abstract fun encodeCandidate(value: Issue756BenchmarkData, target: ByteBuf)

    @Setup(Level.Trial)
    fun setupTrial() {
        expectedWire = serializeCopiedBaseline(ISSUE756_PAYLOAD)
        check(expectedWire.isNotEmpty()) { "Issue 756 benchmark wire must not be empty." }
        check(expectedWire.size < ISSUE756_TARGET_CAPACITY - ISSUE756_START_INDEX) {
            "Issue 756 benchmark payload exceeds fixed target headroom."
        }
        target = when (targetKind) {
            Issue756TargetKind.HEAP -> PooledByteBufAllocator.DEFAULT.heapBuffer(
                ISSUE756_TARGET_CAPACITY,
                ISSUE756_TARGET_CAPACITY,
            )
            Issue756TargetKind.DIRECT -> PooledByteBufAllocator.DEFAULT.directBuffer(
                ISSUE756_TARGET_CAPACITY,
                ISSUE756_TARGET_CAPACITY,
            )
        }
        initialCapacity = target.capacity()
        initialMaxCapacity = target.maxCapacity()
        requireIssue756PooledTarget(target, targetKind)
        verifyTargetIdentity()
    }

    @Setup(Level.Invocation)
    fun resetInvocation() {
        verifyTargetIdentity()
        target.setIndex(ISSUE756_READER_INDEX, ISSUE756_START_INDEX)
        target.setByte(ISSUE756_PREFIX_INDEX, ISSUE756_PREFIX.toInt())
        target.setByte(ISSUE756_START_INDEX, ISSUE756_SENTINEL.toInt())
        target.markReaderIndex()
        target.markWriterIndex()
        verifyInvocationStart()
    }

    @TearDown(Level.Trial)
    fun tearDownTrial() {
        if (::target.isInitialized && target.refCnt() > 0) {
            target.release()
        }
    }

    fun runCopiedBaseline(blackhole: Blackhole) {
        verifyInvocationStart()
        val wire = serializeCopiedBaseline(ISSUE756_PAYLOAD)
        target.writeBytes(wire)
        consumeResult(wire.size, blackhole)
    }

    fun runCandidate(blackhole: Blackhole) {
        verifyInvocationStart()
        encodeCandidate(ISSUE756_PAYLOAD, target)
        val written = target.writerIndex() - ISSUE756_START_INDEX
        consumeResult(written, blackhole)
    }

    private fun consumeResult(written: Int, blackhole: Blackhole) {
        check(written == expectedWire.size) { "Issue 756 benchmark wire count drifted." }
        check(target.writerIndex() == ISSUE756_START_INDEX + written) {
            "Issue 756 benchmark writer index drifted."
        }
        verifyTargetIdentity()
        check(target.getByte(ISSUE756_PREFIX_INDEX) == ISSUE756_PREFIX) {
            "Issue 756 benchmark prefix changed."
        }
        blackhole.consume(written)
        blackhole.consume(target.getByte(ISSUE756_START_INDEX + written - 1))
    }

    private fun verifyInvocationStart() {
        verifyTargetIdentity()
        check(target.readerIndex() == ISSUE756_READER_INDEX) { "Issue 756 reader index was not reset." }
        check(target.writerIndex() == ISSUE756_START_INDEX) { "Issue 756 writer index was not reset." }
        check(target.writableBytes() == initialCapacity - ISSUE756_START_INDEX) {
            "Issue 756 target headroom drifted."
        }
        check(target.getByte(ISSUE756_PREFIX_INDEX) == ISSUE756_PREFIX) {
            "Issue 756 benchmark prefix was not reset."
        }
        check(target.getByte(ISSUE756_START_INDEX) == ISSUE756_SENTINEL) {
            "Issue 756 benchmark sentinel was not reset."
        }
    }

    private fun verifyTargetIdentity() {
        check(target.capacity() == initialCapacity) { "Issue 756 target capacity changed." }
        check(target.maxCapacity() == initialMaxCapacity) { "Issue 756 target maxCapacity changed." }
        check(initialCapacity == ISSUE756_TARGET_CAPACITY) { "Issue 756 target initial capacity drifted." }
        check(initialMaxCapacity == ISSUE756_TARGET_CAPACITY) { "Issue 756 target maxCapacity is not fixed." }
        check(target.refCnt() == 1) { "Issue 756 target reference count changed." }
        check(target.isDirect == (targetKind == Issue756TargetKind.DIRECT)) {
            "Issue 756 target allocator kind changed."
        }
    }
}

internal fun requireIssue756PooledTarget(target: ByteBuf, targetKind: Issue756TargetKind): String {
    val allocator = target.alloc()
    check(allocator is PooledByteBufAllocator) { "Issue 756 target allocator must be pooled." }
    val metric = allocator.metric()
    val arenaCount = when (targetKind) {
        Issue756TargetKind.HEAP   -> metric.numHeapArenas()
        Issue756TargetKind.DIRECT -> metric.numDirectArenas()
    }
    check(arenaCount > 0) { "Issue 756 target allocator has no ${targetKind.name.lowercase()} arenas." }
    var root = target
    while (true) {
        val unwrapped = root.unwrap() ?: break
        if (unwrapped === root) break
        root = unwrapped
    }
    val bufferClass = root.javaClass.name
    check(".Pooled" in bufferClass) { "Issue 756 target is not backed by a pooled buffer: $bufferClass" }
    return bufferClass
}

abstract class Issue756BinaryState(
    targetKind: Issue756TargetKind,
    protected val serializer: BinarySerializer,
): Issue756TargetState(targetKind) {
    private val codec = LettuceBinaryCodec<Issue756BenchmarkData>(serializer)

    override fun serializeCopiedBaseline(value: Issue756BenchmarkData): ByteArray =
        serializer.serialize(value)

    override fun encodeCandidate(value: Issue756BenchmarkData, target: ByteBuf) {
        codec.encodeValue(value, target)
    }
}

abstract class Issue756JsonState(
    targetKind: Issue756TargetKind,
    protected val serializer: JsonSerializer,
): Issue756TargetState(targetKind) {
    private val codec = LettuceJsonCodec(serializer, Issue756BenchmarkData::class.java)

    override fun serializeCopiedBaseline(value: Issue756BenchmarkData): ByteArray =
        serializer.serialize(value)

    override fun encodeCandidate(value: Issue756BenchmarkData, target: ByteBuf) {
        codec.encodeValue(value, target)
    }
}

@State(Scope.Thread)
class JdkHeapState: Issue756BinaryState(Issue756TargetKind.HEAP, JdkBinarySerializer())

@State(Scope.Thread)
class JdkDirectState: Issue756BinaryState(Issue756TargetKind.DIRECT, JdkBinarySerializer())

@State(Scope.Thread)
class KryoHeapState: Issue756BinaryState(Issue756TargetKind.HEAP, KryoBinarySerializer())

@State(Scope.Thread)
class KryoDirectState: Issue756BinaryState(Issue756TargetKind.DIRECT, KryoBinarySerializer())

@State(Scope.Thread)
class Jackson2HeapState: Issue756JsonState(Issue756TargetKind.HEAP, jackson2Serializer())

@State(Scope.Thread)
class Jackson2DirectState: Issue756JsonState(Issue756TargetKind.DIRECT, jackson2Serializer())

@State(Scope.Thread)
class Jackson3HeapState: Issue756JsonState(Issue756TargetKind.HEAP, JacksonSerializer())

@State(Scope.Thread)
class Jackson3DirectState: Issue756JsonState(Issue756TargetKind.DIRECT, JacksonSerializer())

internal fun jackson2Serializer(): JsonSerializer =
    Class.forName("io.bluetape4k.jackson.JacksonSerializer")
        .getDeclaredConstructor()
        .newInstance() as JsonSerializer

/**
 * Exact issue #756 target-handoff allocation matrix.
 *
 * Every backend and target kind has a frozen `serialize -> ByteArray -> writeBytes` baseline paired with the
 * caller-owned Lettuce target overload. The target is pre-sized and reused; only handoff work is timed.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(2)
@Threads(1)
open class LettuceCodecBenchmark {

    @Benchmark
    fun jdkHeapCopiedBaseline(state: JdkHeapState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun jdkHeapCandidate(state: JdkHeapState, blackhole: Blackhole) =
        state.runCandidate(blackhole)

    @Benchmark
    fun jdkDirectCopiedBaseline(state: JdkDirectState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun jdkDirectCandidate(state: JdkDirectState, blackhole: Blackhole) =
        state.runCandidate(blackhole)

    @Benchmark
    fun kryoHeapCopiedBaseline(state: KryoHeapState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun kryoHeapCandidate(state: KryoHeapState, blackhole: Blackhole) =
        state.runCandidate(blackhole)

    @Benchmark
    fun kryoDirectCopiedBaseline(state: KryoDirectState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun kryoDirectCandidate(state: KryoDirectState, blackhole: Blackhole) =
        state.runCandidate(blackhole)

    @Benchmark
    fun jackson2HeapCopiedBaseline(state: Jackson2HeapState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun jackson2HeapCandidate(state: Jackson2HeapState, blackhole: Blackhole) =
        state.runCandidate(blackhole)

    @Benchmark
    fun jackson2DirectCopiedBaseline(state: Jackson2DirectState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun jackson2DirectCandidate(state: Jackson2DirectState, blackhole: Blackhole) =
        state.runCandidate(blackhole)

    @Benchmark
    fun jackson3HeapCopiedBaseline(state: Jackson3HeapState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun jackson3HeapCandidate(state: Jackson3HeapState, blackhole: Blackhole) =
        state.runCandidate(blackhole)

    @Benchmark
    fun jackson3DirectCopiedBaseline(state: Jackson3DirectState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun jackson3DirectCandidate(state: Jackson3DirectState, blackhole: Blackhole) =
        state.runCandidate(blackhole)
}
