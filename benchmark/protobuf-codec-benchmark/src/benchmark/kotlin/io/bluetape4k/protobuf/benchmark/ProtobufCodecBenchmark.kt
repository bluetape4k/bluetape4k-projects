package io.bluetape4k.protobuf.benchmark

import io.bluetape4k.protobuf.benchmark.messages.BenchmarkMessage
import io.bluetape4k.protobuf.benchmark.messages.benchmarkMessage
import io.bluetape4k.protobuf.serializers.ProtobufSerializer
import io.bluetape4k.protobuf.serializers.redis.AnyMessage
import io.bluetape4k.protobuf.serializers.redis.RedissonProtobufCodec
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup
import java.io.Serializable
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
class ProtobufCodecBenchmark {

    private val redissonCodec = RedissonProtobufCodec()
    private val serializer = ProtobufSerializer()

    private lateinit var message: BenchmarkMessage
    private lateinit var fallbackPayload: FallbackPayload
    private lateinit var encodedMessage: ByteArray

    @Setup
    fun setup() {
        message = benchmarkMessage {
            id = 42L
            payload = "protobuf-payload-".repeat(128)
        }
        fallbackPayload = FallbackPayload(
            id = 42L,
            name = "fallback-payload",
            values = List(64) { "value-$it" }
        )
        redissonCodec.valueEncoder.encode(message).useAndRelease { buffer ->
            encodedMessage = ByteArray(buffer.readableBytes()).also { buffer.readBytes(it) }
        }
    }

    @Benchmark
    fun redissonProtobufEncode(): Int =
        redissonCodec.valueEncoder.encode(message).useAndRelease { buffer ->
            buffer.readableBytes()
        }

    @Benchmark
    fun redissonProtobufEncodeByteArrayWrappedBaseline(): Int {
        val bytes = AnyMessage.pack(message).toByteArray()
        return io.netty.buffer.Unpooled.wrappedBuffer(bytes).useAndRelease { buffer ->
            buffer.readableBytes()
        }
    }

    @Benchmark
    fun redissonProtobufDecode(): Any =
        io.netty.buffer.Unpooled.wrappedBuffer(encodedMessage).useAndRelease { buffer ->
            redissonCodec.valueDecoder.decode(buffer, null)
        }

    @Benchmark
    fun protobufSerializerEncode(): Int =
        serializer.serialize(message).size

    @Benchmark
    fun protobufSerializerFallbackEncode(): Int =
        serializer.serialize(fallbackPayload).size

    data class FallbackPayload(
        val id: Long,
        val name: String,
        val values: List<String>,
    ): Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}

private inline fun <T> io.netty.buffer.ByteBuf.useAndRelease(block: (io.netty.buffer.ByteBuf) -> T): T =
    try {
        block(this)
    } finally {
        release()
    }
