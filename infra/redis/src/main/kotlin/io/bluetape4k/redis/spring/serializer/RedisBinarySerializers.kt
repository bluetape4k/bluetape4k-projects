package io.bluetape4k.redis.spring.serializer

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.support.unsafeLazy

/**
 * Spring Data Redis 에서 사용하는 [org.springframework.data.redis.serializer.RedisSerializer]의
 * 다양한 구현체를 제공합니다.
 */
object RedisBinarySerializers {

    val Jdk by unsafeLazy { RedisBinarySerializer(BinarySerializers.Jdk) }
    val Kryo by unsafeLazy { RedisBinarySerializer(BinarySerializers.Kryo) }
    val Fory by unsafeLazy { RedisBinarySerializer(BinarySerializers.Fory) }

    val Gzip by unsafeLazy { RedisCompressSerializer(Compressors.GZip) }
    val LZ4 by unsafeLazy { RedisCompressSerializer(Compressors.LZ4) }
    val Snappy by unsafeLazy { RedisCompressSerializer(Compressors.Snappy) }
    val Zstd by unsafeLazy { RedisCompressSerializer(Compressors.Zstd) }

    val GzipJdk by unsafeLazy { RedisBinarySerializer(BinarySerializers.GZipJdk) }
    val LZ4Jdk by unsafeLazy { RedisBinarySerializer(BinarySerializers.LZ4Jdk) }
    val SnappyJdk by unsafeLazy { RedisBinarySerializer(BinarySerializers.SnappyJdk) }
    val ZstdJdk by unsafeLazy { RedisBinarySerializer(BinarySerializers.ZstdJdk) }

    val GzipKryo by unsafeLazy { RedisBinarySerializer(BinarySerializers.GZipKryo) }
    val LZ4Kryo by unsafeLazy { RedisBinarySerializer(BinarySerializers.LZ4Kryo) }
    val SnappyKryo by unsafeLazy { RedisBinarySerializer(BinarySerializers.SnappyKryo) }
    val ZstdKryo by unsafeLazy { RedisBinarySerializer(BinarySerializers.ZstdKryo) }

    val GzipFory by unsafeLazy { RedisBinarySerializer(BinarySerializers.GZipFory) }
    val LZ4Fory by unsafeLazy { RedisBinarySerializer(BinarySerializers.LZ4Fory) }
    val SnappyFory by unsafeLazy { RedisBinarySerializer(BinarySerializers.SnappyFory) }
    val ZstdFory by unsafeLazy { RedisBinarySerializer(BinarySerializers.ZstdFory) }

}
