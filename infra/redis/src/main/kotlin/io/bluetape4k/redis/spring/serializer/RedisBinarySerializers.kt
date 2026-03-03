package io.bluetape4k.redis.spring.serializer

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers

/**
 * Spring Data Redis 에서 사용하는 [org.springframework.data.redis.serializer.RedisSerializer]의
 * 다양한 구현체를 제공합니다.
 */
object RedisBinarySerializers {

    val Jdk by lazy { RedisBinarySerializer(BinarySerializers.Jdk) }
    val Kryo by lazy { RedisBinarySerializer(BinarySerializers.Kryo) }
    val Fory by lazy { RedisBinarySerializer(BinarySerializers.Fory) }

    val Gzip by lazy { RedisCompressSerializer(Compressors.GZip) }
    val LZ4 by lazy { RedisCompressSerializer(Compressors.LZ4) }
    val Snappy by lazy { RedisCompressSerializer(Compressors.Snappy) }
    val Zstd by lazy { RedisCompressSerializer(Compressors.Zstd) }

    val GzipJdk by lazy { RedisBinarySerializer(BinarySerializers.GZipJdk) }
    val LZ4Jdk by lazy { RedisBinarySerializer(BinarySerializers.LZ4Jdk) }
    val SnappyJdk by lazy { RedisBinarySerializer(BinarySerializers.SnappyJdk) }
    val ZstdJdk by lazy { RedisBinarySerializer(BinarySerializers.ZstdJdk) }

    val GzipKryo by lazy { RedisBinarySerializer(BinarySerializers.GZipKryo) }
    val LZ4Kryo by lazy { RedisBinarySerializer(BinarySerializers.LZ4Kryo) }
    val SnappyKryo by lazy { RedisBinarySerializer(BinarySerializers.SnappyKryo) }
    val ZstdKryo by lazy { RedisBinarySerializer(BinarySerializers.ZstdKryo) }

    val GzipFory by lazy { RedisBinarySerializer(BinarySerializers.GZipFory) }
    val LZ4Fory by lazy { RedisBinarySerializer(BinarySerializers.LZ4Fory) }
    val SnappyFory by lazy { RedisBinarySerializer(BinarySerializers.SnappyFory) }
    val ZstdFory by lazy { RedisBinarySerializer(BinarySerializers.ZstdFory) }

}
