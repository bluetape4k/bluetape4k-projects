package io.bluetape4k.kafka.codec

import io.bluetape4k.annotations.BluetapeDelicateApi
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import org.apache.kafka.common.header.Headers

/**
 * [BinarySerializer]를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = KryoKafkaCodec()
 * val data = listOf("a", "b", "c")
 * val bytes = codec.serialize("topic", null, data)
 * val restored = codec.deserialize("topic", null, bytes)
 * // restored == listOf("a", "b", "c")
 * ```
 */
abstract class BinaryKafkaCodec(
    private val serializer: BinarySerializer,
): AbstractKafkaCodec<Any?>() {

    override fun doSerialize(topic: String?, headers: Headers?, graph: Any?): ByteArray {
        return serializer.serialize(graph)
    }

    override fun doDeserialize(topic: String?, headers: Headers?, bytes: ByteArray): Any? {
        return serializer.deserialize(bytes)
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
