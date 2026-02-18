package io.bluetape4k.kafka.codec

/**
 * 다양한 Kafka Codec 인스턴스를 제공하는 Object입니다.
 *
 * 문자열, 바이트 배열, JSON 직렬화 및 다양한 바이너리 직렬화(JDK, Kryo, FST)와
n * 압축 알고리즘(LZ4, Snappy, Zstd)을 조합한 Codec을 제공합니다.
 *
 * 사용 예시:
 * ```kotlin
 * // 문자열 Codec 사용
 * val stringCodec = KafkaCodecs.String
 *
 * // JSON Codec 사용
 * val jacksonCodec = KafkaCodecs.Jackson
 *
 * // LZ4 압축 + Kryo 직렬화 Codec 사용
 * val lz4KryoCodec = KafkaCodecs.Lz4Kryo
 * ```
 *
 * @see KafkaCodec
 * @see StringKafkaCodec
 * @see JacksonKafkaCodec
 * @see BinaryKafkaCodec
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
