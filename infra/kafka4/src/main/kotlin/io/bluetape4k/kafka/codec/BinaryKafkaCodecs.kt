package io.bluetape4k.kafka.codec

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
 * JDK 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = JdkKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 */
class JdkKafkaCodec: BinaryKafkaCodec(BinarySerializers.Jdk)

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
 * Fory 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = ForyKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 */
class ForyKafkaCodec: BinaryKafkaCodec(BinarySerializers.Fory)

/**
 * LZ4 압축 + JDK 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = LZ4JdkKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 */
class LZ4JdkKafkaCodec: BinaryKafkaCodec(BinarySerializers.LZ4Jdk)

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
 * LZ4 압축 + Fory 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = LZ4ForyKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 */
class LZ4ForyKafkaCodec: BinaryKafkaCodec(BinarySerializers.LZ4Fory)

/**
 * Snappy 압축 + JDK 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = SnappyJdkKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 */
class SnappyJdkKafkaCodec: BinaryKafkaCodec(BinarySerializers.SnappyJdk)

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
 * Snappy 압축 + Fory 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = SnappyForyKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 */
class SnappyForyKafkaCodec: BinaryKafkaCodec(BinarySerializers.SnappyFory)


/**
 * Zstd 압축 + JDK 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = ZstdJdkKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 */
class ZstdJdkKafkaCodec: BinaryKafkaCodec(BinarySerializers.ZstdJdk)

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
 * Zstd 압축 + Fory 직렬화를 이용한 Kafka Codec
 *
 * ```kotlin
 * val codec = ZstdForyKafkaCodec()
 * val bytes = codec.serialize("topic", null, "hello")
 * val result = codec.deserialize("topic", null, bytes)
 * // result == "hello"
 * ```
 */
class ZstdForyKafkaCodec: BinaryKafkaCodec(BinarySerializers.ZstdFory)
