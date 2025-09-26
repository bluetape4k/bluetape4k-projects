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

    @Deprecated("Fury is deprecated. Use Fory instead", ReplaceWith("Fory"))
    val Fury by lazy { FuryKafkaCodec() }
    val Fory by lazy { ForyKafkaCodec() }

    val LZ4Jdk by lazy { LZ4JdkKafkaCodec() }
    val Lz4Kryo by lazy { LZ4KryoKafkaCodec() }

    @Deprecated("LZ4Fury is deprecated. Use LZ4Fory instead", ReplaceWith("Lz4Fory"))
    val Lz4Fury by lazy { LZ4FuryKafkaCodec() }
    val Lz4Fory by lazy { LZ4ForyKafkaCodec() }

    val SnappyJdk by lazy { SnappyJdkKafkaCodec() }
    val SnappyKryo by lazy { SnappyKryoKafkaCodec() }

    @Deprecated("SnappyFury is deprecated. Use SnappyFory instead", ReplaceWith("SnappyFory"))
    val SnappyFury by lazy { SnappyFuryKafkaCodec() }
    val SnappyFory by lazy { SnappyForyKafkaCodec() }

    val ZstdJdk by lazy { ZstdJdkKafkaCodec() }
    val ZstdKryo by lazy { ZstdKryoKafkaCodec() }

    @Deprecated("ZstdFury is deprecated. Use ZstdFory instead", ReplaceWith("ZstdFory"))
    val ZstdFury by lazy { ZstdFuryKafkaCodec() }
    val ZstdFory by lazy { ZstdForyKafkaCodec() }
}
