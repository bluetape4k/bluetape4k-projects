package io.bluetape4k.redis.lettuce.benchmark

import io.bluetape4k.io.serializer.ForyBinarySerializer
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@org.openjdk.jmh.annotations.State(org.openjdk.jmh.annotations.Scope.Thread)
class Issue756ForyHeapState:
    Issue756BinaryState(Issue756TargetKind.HEAP, ForyBinarySerializer())

@org.openjdk.jmh.annotations.State(org.openjdk.jmh.annotations.Scope.Thread)
class Issue756ForyDirectState:
    Issue756BinaryState(Issue756TargetKind.DIRECT, ForyBinarySerializer())

@org.openjdk.jmh.annotations.State(org.openjdk.jmh.annotations.Scope.Thread)
class Issue756FastForyHeapState:
    Issue756BinaryState(Issue756TargetKind.HEAP, ForyBinarySerializer.fast())

@org.openjdk.jmh.annotations.State(org.openjdk.jmh.annotations.Scope.Thread)
class Issue756FastForyDirectState:
    Issue756BinaryState(Issue756TargetKind.DIRECT, ForyBinarySerializer.fast())

/**
 * Issue #756 Apache Fory caller-owned Lettuce target handoff matrix.
 *
 * Each candidate is paired with the frozen `serialize -> ByteArray -> writeBytes` baseline from
 * [Issue756TargetState]. Allocation, reset, and teardown stay outside the timed methods.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2, jvmArgs = ["-Xms1g", "-Xmx1g", "-XX:+UseG1GC"])
@Threads(1)
open class Issue756ForyCodecBenchmark {

    @Benchmark
    fun foryHeapCopiedBaseline(state: Issue756ForyHeapState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun foryHeapCandidate(state: Issue756ForyHeapState, blackhole: Blackhole) =
        state.runCandidate(blackhole)

    @Benchmark
    fun foryDirectCopiedBaseline(state: Issue756ForyDirectState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun foryDirectCandidate(state: Issue756ForyDirectState, blackhole: Blackhole) =
        state.runCandidate(blackhole)

    @Benchmark
    fun fastForyHeapCopiedBaseline(state: Issue756FastForyHeapState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun fastForyHeapCandidate(state: Issue756FastForyHeapState, blackhole: Blackhole) =
        state.runCandidate(blackhole)

    @Benchmark
    fun fastForyDirectCopiedBaseline(state: Issue756FastForyDirectState, blackhole: Blackhole) =
        state.runCopiedBaseline(blackhole)

    @Benchmark
    fun fastForyDirectCandidate(state: Issue756FastForyDirectState, blackhole: Blackhole) =
        state.runCandidate(blackhole)
}
