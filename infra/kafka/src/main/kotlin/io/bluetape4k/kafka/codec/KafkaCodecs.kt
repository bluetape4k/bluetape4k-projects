package io.bluetape4k.kafka.codec

/**
 * [KafkaCodec]을 제공하는 Object
 */
object KafkaCodecs {

    val String by lazy { StringKafkaCodec() }
    val ByteArray by lazy { ByteArrayKafkaCodec() }

    val Jackson by lazy { JacksonKafkaCodec() }

    val Jdk by lazy { JdkKafkaCodec() }
    val Kryo by lazy { KryoKafkaCodec() }
    val Fory by lazy { ForyKafkaCodec() }

    val LZ4Jdk by lazy { LZ4JdkKafkaCodec() }
    val Lz4Kryo by lazy { LZ4KryoKafkaCodec() }
    val Lz4Fory by lazy { LZ4ForyKafkaCodec() }

    val SnappyJdk by lazy { SnappyJdkKafkaCodec() }
    val SnappyKryo by lazy { SnappyKryoKafkaCodec() }
    val SnappyFory by lazy { SnappyForyKafkaCodec() }

    val ZstdJdk by lazy { ZstdJdkKafkaCodec() }
    val ZstdKryo by lazy { ZstdKryoKafkaCodec() }
    val ZstdFory by lazy { ZstdForyKafkaCodec() }
}
