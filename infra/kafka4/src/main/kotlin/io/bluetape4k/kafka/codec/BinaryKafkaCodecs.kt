package io.bluetape4k.kafka.codec

import io.bluetape4k.annotations.BluetapeDelicateApi
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import org.apache.kafka.common.header.Headers
import java.nio.ByteBuffer

/**
 * Kafka codec backed by a [BinarySerializer].
 *
 * Standard Kafka calls retain their required [ByteArray] boundary. [BufferAwareKafkaCodec] methods delegate to the
 * serializer's caller-owned buffer API without a Kafka-layer array conversion. Allocation behavior still depends on
 * the concrete serializer because interface-default buffer methods may be allocating compatibility fallbacks.
 *
 * Buffer deserialization uses the same WARN-and-null poison-pill policy as standard Kafka deserialization while
 * preserving coroutine cancellation and fatal JVM errors.
 */
abstract class BinaryKafkaCodec(
    private val serializer: BinarySerializer,
): AbstractKafkaCodec<Any?>(), BufferAwareKafkaCodec<Any?> {

    override fun doSerialize(topic: String?, headers: Headers?, graph: Any?): ByteArray =
        serializer.serialize(graph)

    override fun doDeserialize(topic: String?, headers: Headers?, bytes: ByteArray): Any? =
        serializer.deserialize(bytes)

    override fun serializeTo(
        topic: String?,
        headers: Headers?,
        data: Any,
        target: ByteBuffer,
    ): Int {
        // Match the standard path: a committed header is not rolled back when serializer work fails.
        if (writeValueTypeHeader) setValueType(headers, data.javaClass)
        return serializer.serializeTo(data, target)
    }

    override fun deserializeFrom(topic: String?, headers: Headers?, source: ByteBuffer): Any? =
        deserializeSafely(topic, headers, source.remaining()) {
            serializer.deserializeFrom<Any>(source)
        }
}

/**
 * Kryo 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = KryoKafkaCodec()
 * val bytes = codec.serialize("topic", null, 42)
 * val result = codec.deserialize("topic", null, bytes)
 * // result == 42
 * ```
 */
class KryoKafkaCodec: BinaryKafkaCodec(BinarySerializers.Kryo)

/**
 * Kafka codec backed by the default Fory binary serializer.
 *
 * ```kotlin
 * val codec = ForyKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 *
 * ## Security
 *
 * This codec uses `BinarySerializers.Fory`, whose default Fory configuration
 * allows unregistered classes during deserialization. Use it only for trusted
 * topics and brokers, or provide a codec backed by a class-registration-enforced
 * `ForyBinarySerializer` for shared or external inputs.
 */
@BluetapeDelicateApi
class ForyKafkaCodec: BinaryKafkaCodec(BinarySerializers.Fory)

/**
 * LZ4 압축 + Kryo 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = LZ4KryoKafkaCodec()
 * val bytes = codec.serialize("topic", null, listOf(1, 2, 3))
 * val result = codec.deserialize("topic", null, bytes)
 * // result == listOf(1, 2, 3)
 * ```
 */
class LZ4KryoKafkaCodec: BinaryKafkaCodec(BinarySerializers.LZ4Kryo)

/**
 * Kafka codec backed by LZ4 compression and the default Fory binary serializer.
 *
 * ```kotlin
 * val codec = LZ4ForyKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 *
 * ## Security
 *
 * This codec uses `BinarySerializers.LZ4Fory`, which delegates to the default
 * Fory serializer and allows unregistered classes during deserialization. Use
 * it only for trusted topics and brokers.
 */
@BluetapeDelicateApi
class LZ4ForyKafkaCodec: BinaryKafkaCodec(BinarySerializers.LZ4Fory)

/**
 * Snappy 압축 + Kryo 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = SnappyKryoKafkaCodec()
 * val bytes = codec.serialize("topic", null, mapOf("k" to "v"))
 * val result = codec.deserialize("topic", null, bytes)
 * // result is a Map with k -> v
 * ```
 */
class SnappyKryoKafkaCodec: BinaryKafkaCodec(BinarySerializers.SnappyKryo)

/**
 * Kafka codec backed by Snappy compression and the default Fory binary serializer.
 *
 * ```kotlin
 * val codec = SnappyForyKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 *
 * ## Security
 *
 * This codec uses `BinarySerializers.SnappyFory`, which delegates to the default
 * Fory serializer and allows unregistered classes during deserialization. Use
 * it only for trusted topics and brokers.
 */
@BluetapeDelicateApi
class SnappyForyKafkaCodec: BinaryKafkaCodec(BinarySerializers.SnappyFory)

/**
 * Zstd 압축 + Kryo 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = ZstdKryoKafkaCodec()
 * val bytes = codec.serialize("topic", null, listOf("a", "b"))
 * val result = codec.deserialize("topic", null, bytes)
 * // result == listOf("a", "b")
 * ```
 */
class ZstdKryoKafkaCodec: BinaryKafkaCodec(BinarySerializers.ZstdKryo)

/**
 * Kafka codec backed by Zstd compression and the default Fory binary serializer.
 *
 * ```kotlin
 * val codec = ZstdForyKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 *
 * ## Security
 *
 * This codec uses `BinarySerializers.ZstdFory`, which delegates to the default
 * Fory serializer and allows unregistered classes during deserialization. Use
 * it only for trusted topics and brokers.
 */
@BluetapeDelicateApi
class ZstdForyKafkaCodec: BinaryKafkaCodec(BinarySerializers.ZstdFory)
