package io.bluetape4k.protobuf.benchmark

import com.google.protobuf.Any as ProtoAny
import com.google.protobuf.Message
import io.bluetape4k.protobuf.benchmark.messages.BenchmarkMessage
import io.bluetape4k.protobuf.benchmark.messages.benchmarkMessage
import io.bluetape4k.protobuf.serializers.ProtobufSerializer
import io.bluetape4k.protobuf.serializers.redis.RedissonProtobufCodec
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.redisson.client.handler.State
import java.io.Serializable
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object ProtobufBenchmarkMatrix {
    const val VERSION = "issue-757-v1"
    const val TARGET_HEADROOM = 32
    const val TARGET_START = 3
    const val PAYLOAD_IDENTITY = "BenchmarkMessage:id=42;payload=protobuf-payload-*128"
    val expectedMethods = setOf(
        "serializerEncodeByteArray",
        "serializerEncodeHeapOptimized",
        "serializerEncodeDirectOptimized",
        "serializerDecodeByteArray",
        "serializerDecodeHeapOptimized",
        "serializerDecodeDirectOptimized",
        "redissonDecodeCopiedByteArray",
        "redissonDecodeContiguousOptimized",
        "redissonDecodeCompositeCompatibility",
        "trustedFallbackEncodeByteArray",
        "trustedFallbackEncodeBufferCompatibility",
        "trustedFallbackDecodeByteArray",
        "trustedFallbackDecodeBufferCompatibility",
    )
    val claimEligible = expectedMethods.filterTo(mutableSetOf()) { it.endsWith("Optimized") }
}

data class FallbackPayload(val id: Long, val values: List<String>): Serializable

internal fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { ch ->
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
        }
    }
    append('"')
}

private fun jsonStrings(values: List<String>): String =
    values.joinToString(prefix = "[", postfix = "]", separator = ",") { jsonString(it) }

private fun canonicalConfigJson(
    allowedPrefixes: List<String>,
    directCapacity: Int,
    directInitialPosition: Int,
    heapCapacity: Int,
    heapInitialPosition: Int,
    matrixVersion: String,
    methods: List<String>,
    payloadIdentity: String,
    payloadSha256: String,
    redissonCodecClass: String,
    serializerClass: String,
    targetHeadroom: Int,
    targetStart: Int,
): String = buildString {
    append("{\"allowed_class_prefixes\":").append(jsonStrings(allowedPrefixes))
    append(",\"direct_capacity\":").append(directCapacity)
    append(",\"direct_initial_position\":").append(directInitialPosition)
    append(",\"heap_capacity\":").append(heapCapacity)
    append(",\"heap_initial_position\":").append(heapInitialPosition)
    append(",\"matrix_version\":").append(jsonString(matrixVersion))
    append(",\"methods\":").append(jsonStrings(methods))
    append(",\"payload_identity\":").append(jsonString(payloadIdentity))
    append(",\"payload_sha256\":").append(jsonString(payloadSha256))
    append(",\"redisson_codec_class\":").append(jsonString(redissonCodecClass))
    append(",\"serializer_class\":").append(jsonString(serializerClass))
    append(",\"target_headroom\":").append(targetHeadroom)
    append(",\"target_start\":").append(targetStart)
    append('}')
}

internal fun ByteBuffer.targetBytes(written: Int): ByteArray {
    check(written >= 0) { "written must be non-negative: $written" }
    val start = ProtobufBenchmarkMatrix.TARGET_START
    val end = start + written
    check(position() == end) { "Unexpected target position: position=${position()}, expected=$end" }
    check(end <= capacity()) { "Written bytes exceed target capacity: end=$end, capacity=${capacity()}" }
    val source = duplicate().position(start).limit(end)
    return ByteArray(written).also(source::get)
}

class ProtobufCodecBenchmarkFixture {
    val payload: BenchmarkMessage = benchmarkMessage {
        id = 42L
        payload = "protobuf-payload-".repeat(128)
    }
    private val serializer = ProtobufSerializer()
    private val trusted = ProtobufSerializer.trustedInternalProtobuf()
    private val redisson = RedissonProtobufCodec()
    private val redissonBaselineClasses = ConcurrentHashMap<String, Class<out Message>>()
    private val fallback = FallbackPayload(42L, List(64) { "value-$it" })
    private val wire = serializer.serialize(payload)
    private val fallbackWire = trusted.serialize(fallback)
    private val heapTarget = ByteBuffer.allocate(wire.size + ProtobufBenchmarkMatrix.TARGET_HEADROOM)
    private val directTarget = ByteBuffer.allocateDirect(wire.size + ProtobufBenchmarkMatrix.TARGET_HEADROOM)
    private val fallbackTarget = ByteBuffer.allocate(fallbackWire.size + ProtobufBenchmarkMatrix.TARGET_HEADROOM)
    private val heapSource = ByteBuffer.wrap(wire)
    private val directSource = ByteBuffer.allocateDirect(wire.size).apply { put(wire).flip() }
    private val fallbackSource = ByteBuffer.wrap(fallbackWire)
    private val redissonInput: ByteBuf = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(wire))

    val payloadSha256: String = MessageDigest.getInstance("SHA-256")
        .digest(wire)
        .joinToString("") { "%02x".format(it) }
    val wireSize: Int get() = wire.size
    val configIdentity: String = canonicalConfigJson(
        allowedPrefixes = ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES.sorted(),
        directCapacity = directTarget.capacity(),
        directInitialPosition = ProtobufBenchmarkMatrix.TARGET_START,
        heapCapacity = heapTarget.capacity(),
        heapInitialPosition = ProtobufBenchmarkMatrix.TARGET_START,
        matrixVersion = ProtobufBenchmarkMatrix.VERSION,
        methods = ProtobufBenchmarkMatrix.expectedMethods.sorted(),
        payloadIdentity = ProtobufBenchmarkMatrix.PAYLOAD_IDENTITY,
        payloadSha256 = payloadSha256,
        redissonCodecClass = RedissonProtobufCodec::class.java.name,
        serializerClass = ProtobufSerializer::class.java.name,
        targetHeadroom = ProtobufBenchmarkMatrix.TARGET_HEADROOM,
        targetStart = ProtobufBenchmarkMatrix.TARGET_START,
    )
    val configSha256: String = MessageDigest.getInstance("SHA-256")
        .digest(configIdentity.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    fun resetInvocation() {
        heapTarget.clear().position(ProtobufBenchmarkMatrix.TARGET_START)
        directTarget.clear().position(ProtobufBenchmarkMatrix.TARGET_START)
        fallbackTarget.clear().position(ProtobufBenchmarkMatrix.TARGET_START)
        heapSource.position(0).limit(wire.size)
        directSource.position(0).limit(wire.size)
        fallbackSource.position(0).limit(fallbackWire.size)
        redissonInput.setIndex(0, wire.size)
    }

    fun serializerEncodeByteArray(): ByteArray = serializer.serialize(payload)
    fun serializerEncodeHeap(): Int = serializer.serializeTo(payload, heapTarget)
    fun serializerEncodeDirect(): Int = serializer.serializeTo(payload, directTarget)
    internal fun serializerHeapTargetBytes(written: Int): ByteArray = heapTarget.targetBytes(written)
    internal fun serializerDirectTargetBytes(written: Int): ByteArray = directTarget.targetBytes(written)
    fun serializerDecodeByteArray(): BenchmarkMessage? = serializer.deserialize(wire)
    fun serializerDecodeHeap(): BenchmarkMessage? = serializer.deserializeFrom(heapSource)
    fun serializerDecodeDirect(): BenchmarkMessage? = serializer.deserializeFrom(directSource)

    fun redissonDecodeCopied(): Any {
        val copied = ByteArray(redissonInput.readableBytes())
        redissonInput.getBytes(redissonInput.readerIndex(), copied)
        val any = ProtoAny.parseFrom(copied)
        val className = any.typeUrl.substringAfterLast("/")
        check(
            ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES.any { prefix ->
                className == prefix || className.startsWith(if (prefix.endsWith('.')) prefix else "$prefix.")
            }
        ) { "Unexpected benchmark Protobuf class: $className" }
        val clazz = redissonBaselineClasses.computeIfAbsent(className) {
            Class.forName(it, false, Thread.currentThread().contextClassLoader)
                .asSubclass(Message::class.java)
        }
        return any.unpack(clazz)
    }

    fun redissonDecodeContiguous(): Any = redisson.valueDecoder.decode(redissonInput, State())

    fun redissonDecodeComposite(): Any {
        val split = wire.size / 2
        val composite = Unpooled.compositeBuffer().addComponents(
            true,
            Unpooled.wrappedBuffer(wire, 0, split),
            Unpooled.wrappedBuffer(wire, split, wire.size - split),
        )
        return try {
            redisson.valueDecoder.decode(composite, State())
        } finally {
            composite.release()
        }
    }

    fun trustedEncodeByteArray(): ByteArray = trusted.serialize(fallback)
    fun trustedEncodeBuffer(): Int = trusted.serializeTo(fallback, fallbackTarget)
    internal fun trustedTargetBytes(written: Int): ByteArray = fallbackTarget.targetBytes(written)
    fun trustedDecodeByteArray(): FallbackPayload? = trusted.deserialize(fallbackWire)
    fun trustedDecodeBuffer(): FallbackPayload? = trusted.deserializeFrom(fallbackSource)

    fun validate() {
        check(wire.isNotEmpty())
        check(payloadSha256.length == 64)
        check(configSha256.length == 64)

        resetInvocation()
        check(serializerEncodeByteArray().contentEquals(wire))

        resetInvocation()
        val heapWritten = serializerEncodeHeap()
        check(heapWritten == wire.size)
        check(serializerHeapTargetBytes(heapWritten).contentEquals(wire))

        resetInvocation()
        val directWritten = serializerEncodeDirect()
        check(directWritten == wire.size)
        check(serializerDirectTargetBytes(directWritten).contentEquals(wire))

        resetInvocation()
        check(serializerDecodeByteArray() == payload)

        resetInvocation()
        check(serializerDecodeHeap() == payload)

        resetInvocation()
        check(serializerDecodeDirect() == payload)

        resetInvocation()
        check(redissonDecodeCopied() == payload)

        resetInvocation()
        check(redissonDecodeContiguous() == payload)

        resetInvocation()
        check(redissonDecodeComposite() == payload)

        resetInvocation()
        check(trustedEncodeByteArray().contentEquals(fallbackWire))

        resetInvocation()
        val fallbackWritten = trustedEncodeBuffer()
        check(fallbackWritten == fallbackWire.size)
        check(trustedTargetBytes(fallbackWritten).contentEquals(fallbackWire))

        resetInvocation()
        check(trustedDecodeByteArray() == fallback)

        resetInvocation()
        check(trustedDecodeBuffer() == fallback)
    }
}
