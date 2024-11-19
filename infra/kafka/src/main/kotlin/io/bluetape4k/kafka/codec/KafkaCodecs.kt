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
    val Fury by lazy { FuryKafkaCodec() }

    val LZ4Jdk by lazy { LZ4JdkKafkaCodec() }
    val Lz4Kryo by lazy { LZ4KryoKafkaCodec() }
    val Lz4Fury by lazy { LZ4FuryKafkaCodec() }

    val SnappyJdk by lazy { SnappyJdkKafkaCodec() }
    val SnappyKryo by lazy { SnappyKryoKafkaCodec() }
    val SnappyFury by lazy { SnappyFuryKafkaCodec() }

    val ZstdJdk by lazy { ZstdJdkKafkaCodec() }
    val ZstdKryo by lazy { ZstdKryoKafkaCodec() }
    val ZstdFury by lazy { ZstdFuryKafkaCodec() }
}
