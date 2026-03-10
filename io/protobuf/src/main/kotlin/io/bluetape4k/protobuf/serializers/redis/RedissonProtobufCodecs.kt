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
 * Redisson 용 Codec.
 * Redisson에서 제공하는 Codec 보다 성능을 빠르게 하는 것들을 포함하고 있습니다.
 */
object RedissonProtobufCodecs: KLogging() {

    val String: Codec by lazy { StringCodec() }
    val Protobuf: Codec by lazy { RedissonProtobufCodec() }

    val GzipProtobuf: Codec by lazy { GzipCodec(Protobuf) }
    val GzipProtobufComposite: Codec by lazy { CompositeCodec(String, GzipProtobuf, GzipProtobuf) }

    val LZ4Protobuf: Codec by lazy { Lz4Codec(Protobuf) }
    val LZ4ProtobufComposite: Codec by lazy { CompositeCodec(String, LZ4Protobuf, LZ4Protobuf) }

    val SnappyProtobuf: Codec by lazy { SnappyCodecV2(Protobuf) }
    val SnappyProtobufComposite: Codec by lazy { CompositeCodec(String, SnappyProtobuf, SnappyProtobuf) }

    val ZstdProtobuf: Codec by lazy { ZstdCodec(Protobuf) }
    val ZstdProtobufComposite: Codec by lazy { CompositeCodec(String, ZstdProtobuf, ZstdProtobuf) }

}
