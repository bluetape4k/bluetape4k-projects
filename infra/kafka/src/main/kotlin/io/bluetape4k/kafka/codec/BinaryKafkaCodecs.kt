package io.bluetape4k.kafka.codec

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import org.apache.kafka.common.header.Headers

/**
 * [BinarySerializer]를 이용한 Kafka Codec
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
 * [BinarySerializers.Jdk]를 이용한 Kafka Codec
 */
class JdkKafkaCodec: BinaryKafkaCodec(BinarySerializers.Jdk)

/**
 * [BinarySerializers.Kryo]를 이용한 Kafka Codec
 */
class KryoKafkaCodec: BinaryKafkaCodec(BinarySerializers.Kryo)

/**
 * [BinarySerializers.Fury]를 이용한 Kafka Codec
 */
class FuryKafkaCodec: BinaryKafkaCodec(BinarySerializers.Fury)

/**
 * [BinarySerializers.LZ4Jdk]를 이용한 Kafka Codec
 */
class LZ4JdkKafkaCodec: BinaryKafkaCodec(BinarySerializers.LZ4Jdk)

/**
 * [BinarySerializers.LZ4Kryo]를 이용한 Kafka Codec
 */
class LZ4KryoKafkaCodec: BinaryKafkaCodec(BinarySerializers.LZ4Kryo)

/**
 * [BinarySerializers.LZ4Fury]를 이용한 Kafka Codec
 */
class LZ4FuryKafkaCodec: BinaryKafkaCodec(BinarySerializers.LZ4Fury)

/**
 * [BinarySerializers.SnappyJdk]를 이용한 Kafka Codec
 */
class SnappyJdkKafkaCodec: BinaryKafkaCodec(BinarySerializers.SnappyJdk)

/**
 * [BinarySerializers.SnappyKryo]를 이용한 Kafka Codec
 */
class SnappyKryoKafkaCodec: BinaryKafkaCodec(BinarySerializers.SnappyKryo)

/**
 * [BinarySerializers.SnappyFury]를 이용한 Kafka Codec
 */
class SnappyFuryKafkaCodec: BinaryKafkaCodec(BinarySerializers.SnappyFury)

/**
 * [BinarySerializers.ZstdJdk]를 이용한 Kafka Codec
 */
class ZstdJdkKafkaCodec: BinaryKafkaCodec(BinarySerializers.ZstdJdk)

/**
 * [BinarySerializers.ZstdKryo]를 이용한 Kafka Codec
 */
class ZstdKryoKafkaCodec: BinaryKafkaCodec(BinarySerializers.ZstdKryo)

/**
 * [BinarySerializers.ZstdFury]를 이용한 Kafka Codec
 */
class ZstdFuryKafkaCodec: BinaryKafkaCodec(BinarySerializers.ZstdFury)
