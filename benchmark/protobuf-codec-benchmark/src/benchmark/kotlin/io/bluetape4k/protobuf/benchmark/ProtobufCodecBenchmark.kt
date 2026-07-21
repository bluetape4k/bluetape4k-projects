package io.bluetape4k.protobuf.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.TearDown
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
class ProtobufCodecBenchmark {
    private lateinit var fixture: ProtobufCodecBenchmarkFixture

    @Param(ProtobufBenchmarkMatrix.VERSION)
    lateinit var matrixVersion: String

    @Param("32")
    var targetHeadroom: Int = 0

    @Param("3")
    var targetStart: Int = 0

    @Setup(Level.Trial)
    fun setup() {
        check(matrixVersion == ProtobufBenchmarkMatrix.VERSION)
        check(targetHeadroom == ProtobufBenchmarkMatrix.TARGET_HEADROOM)
        check(targetStart == ProtobufBenchmarkMatrix.TARGET_START)
        fixture = ProtobufCodecBenchmarkFixture().also { it.validate() }
    }

    @Setup(Level.Invocation)
    fun resetInvocation() = fixture.resetInvocation()

    @TearDown(Level.Trial)
    fun tearDown() = fixture.close()

    @Benchmark fun serializerEncodeByteArray() = fixture.serializerEncodeByteArray()
    @Benchmark fun serializerEncodeHeapOptimized() = fixture.serializerEncodeHeap()
    @Benchmark fun serializerEncodeDirectOptimized() = fixture.serializerEncodeDirect()
    @Benchmark fun serializerDecodeByteArray() = fixture.serializerDecodeByteArray()
    @Benchmark fun serializerDecodeHeapOptimized() = fixture.serializerDecodeHeap()
    @Benchmark fun serializerDecodeDirectOptimized() = fixture.serializerDecodeDirect()
    @Benchmark fun redissonDecodeCopiedByteArray() = fixture.redissonDecodeCopied()
    @Benchmark fun redissonDecodeContiguousOptimized() = fixture.redissonDecodeContiguous()
    @Benchmark fun redissonDecodeCompositeCompatibility() = fixture.redissonDecodeComposite()
    @Benchmark fun trustedFallbackEncodeByteArray() = fixture.trustedEncodeByteArray()
    @Benchmark fun trustedFallbackEncodeBufferCompatibility() = fixture.trustedEncodeBuffer()
    @Benchmark fun trustedFallbackDecodeByteArray() = fixture.trustedDecodeByteArray()
    @Benchmark fun trustedFallbackDecodeBufferCompatibility() = fixture.trustedDecodeBuffer()
    @Benchmark fun lettuceEncodeHeapCopied() = fixture.lettuceEncodeHeapCopied()
    @Benchmark fun lettuceEncodeHeapOptimized() = fixture.lettuceEncodeHeapOptimized()
    @Benchmark fun lettuceEncodeDirectCopied() = fixture.lettuceEncodeDirectCopied()
    @Benchmark fun lettuceEncodeDirectOptimized() = fixture.lettuceEncodeDirectOptimized()
}
