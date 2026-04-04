package io.bluetape4k.protobuf.serializers.redis

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.codec.GzipCodec
import io.bluetape4k.redis.redisson.codec.Lz4Codec
import io.bluetape4k.redis.redisson.codec.ZstdCodec
import org.redisson.client.codec.Codec
import org.redisson.client.codec.StringCodec
import org.redisson.codec.CompositeCodec
import org.redisson.codec.SnappyCodecV2

/**
 * Protobuf 직렬화를 사용하는 Redisson Codec 모음입니다.
 *
 * Redisson에서 제공하는 기본 Codec보다 성능이 빠른 Protobuf 기반 Codec 인스턴스를 제공합니다.
 * 각 Codec은 lazy 초기화되며, 단순/복합(CompositeCodec) 형태로 제공됩니다.
 *
 * ```kotlin
 * val config = Config()
 * config.codec = RedissonProtobufCodecs.LZ4Protobuf
 * // LZ4 압축 + Protobuf 직렬화 Codec 사용
 * ```
 */
object RedissonProtobufCodecs: KLogging() {

    /** 문자열 전용 Codec입니다. */
    val String: Codec by lazy { StringCodec() }

    /**
     * 압축 없이 Protobuf 직렬화만 수행하는 Codec입니다.
     *
     * ```kotlin
     * val codec = RedissonProtobufCodecs.Protobuf
     * // codec != null
     * ```
     */
    val Protobuf: Codec by lazy { RedissonProtobufCodec() }

    /**
     * Gzip 압축 + Protobuf 직렬화 Codec입니다.
     *
     * ```kotlin
     * val codec = RedissonProtobufCodecs.GzipProtobuf
     * // codec != null
     * ```
     */
    val GzipProtobuf: Codec by lazy { GzipCodec(Protobuf) }

    /**
     * 키는 String, 값은 Gzip+Protobuf를 사용하는 CompositeCodec입니다.
     *
     * ```kotlin
     * val codec = RedissonProtobufCodecs.GzipProtobufComposite
     * // codec != null
     * ```
     */
    val GzipProtobufComposite: Codec by lazy { CompositeCodec(String, GzipProtobuf, GzipProtobuf) }

    /**
     * LZ4 압축 + Protobuf 직렬화 Codec입니다.
     *
     * ```kotlin
     * val codec = RedissonProtobufCodecs.LZ4Protobuf
     * // codec != null
     * ```
     */
    val LZ4Protobuf: Codec by lazy { Lz4Codec(Protobuf) }

    /**
     * 키는 String, 값은 LZ4+Protobuf를 사용하는 CompositeCodec입니다.
     *
     * ```kotlin
     * val codec = RedissonProtobufCodecs.LZ4ProtobufComposite
     * // codec != null
     * ```
     */
    val LZ4ProtobufComposite: Codec by lazy { CompositeCodec(String, LZ4Protobuf, LZ4Protobuf) }

    /**
     * Snappy 압축 + Protobuf 직렬화 Codec입니다.
     *
     * ```kotlin
     * val codec = RedissonProtobufCodecs.SnappyProtobuf
     * // codec != null
     * ```
     */
    val SnappyProtobuf: Codec by lazy { SnappyCodecV2(Protobuf) }

    /**
     * 키는 String, 값은 Snappy+Protobuf를 사용하는 CompositeCodec입니다.
     *
     * ```kotlin
     * val codec = RedissonProtobufCodecs.SnappyProtobufComposite
     * // codec != null
     * ```
     */
    val SnappyProtobufComposite: Codec by lazy { CompositeCodec(String, SnappyProtobuf, SnappyProtobuf) }

    /**
     * Zstandard 압축 + Protobuf 직렬화 Codec입니다.
     *
     * ```kotlin
     * val codec = RedissonProtobufCodecs.ZstdProtobuf
     * // codec != null
     * ```
     */
    val ZstdProtobuf: Codec by lazy { ZstdCodec(Protobuf) }

    /**
     * 키는 String, 값은 Zstd+Protobuf를 사용하는 CompositeCodec입니다.
     *
     * ```kotlin
     * val codec = RedissonProtobufCodecs.ZstdProtobufComposite
     * // codec != null
     * ```
     */
    val ZstdProtobufComposite: Codec by lazy { CompositeCodec(String, ZstdProtobuf, ZstdProtobuf) }

}
