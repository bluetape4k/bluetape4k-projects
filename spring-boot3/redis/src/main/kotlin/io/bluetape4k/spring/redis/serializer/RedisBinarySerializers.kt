package io.bluetape4k.spring.redis.serializer

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers

/**
 * Spring Data Redis 에서 사용하는 [org.springframework.data.redis.serializer.RedisSerializer]의
 * 다양한 구현체를 제공합니다.
 *
 * ## 동작/계약
 * - 직렬화 방식(Jdk/Kryo/Fory)과 압축 방식(GZip/LZ4/Snappy/Zstd)을 조합해 제공합니다.
 * - 모든 인스턴스는 `by lazy`로 최초 사용 시 한 번만 생성됩니다.
 * - `RedisTemplate`의 `valueSerializer`나 `hashValueSerializer`에 바로 할당해 사용합니다.
 *
 * ```kotlin
 * val redisTemplate = RedisTemplate<String, Any>()
 * redisTemplate.valueSerializer = RedisBinarySerializers.LZ4Kryo
 * // redisTemplate.opsForValue().set("key", myObj) 시 Kryo 직렬화 + LZ4 압축
 * ```
 */
object RedisBinarySerializers {

    /** JDK 기본 직렬화 */
    val Jdk by lazy { RedisBinarySerializer(BinarySerializers.Jdk) }

    /** Kryo 직렬화 */
    val Kryo by lazy { RedisBinarySerializer(BinarySerializers.Kryo) }

    /** Fory 직렬화 */
    val Fory by lazy { RedisBinarySerializer(BinarySerializers.Fory) }

    /** GZip 압축 전용 (값이 ByteArray일 때) */
    val Gzip by lazy { RedisCompressSerializer(Compressors.GZip) }

    /** LZ4 압축 전용 (값이 ByteArray일 때) */
    val LZ4 by lazy { RedisCompressSerializer(Compressors.LZ4) }

    /** Snappy 압축 전용 (값이 ByteArray일 때) */
    val Snappy by lazy { RedisCompressSerializer(Compressors.Snappy) }

    /** Zstd 압축 전용 (값이 ByteArray일 때) */
    val Zstd by lazy { RedisCompressSerializer(Compressors.Zstd) }

    /** GZip + JDK 직렬화 조합 */
    val GzipJdk by lazy { RedisBinarySerializer(BinarySerializers.GZipJdk) }

    /** LZ4 + JDK 직렬화 조합 */
    val LZ4Jdk by lazy { RedisBinarySerializer(BinarySerializers.LZ4Jdk) }

    /** Snappy + JDK 직렬화 조합 */
    val SnappyJdk by lazy { RedisBinarySerializer(BinarySerializers.SnappyJdk) }

    /** Zstd + JDK 직렬화 조합 */
    val ZstdJdk by lazy { RedisBinarySerializer(BinarySerializers.ZstdJdk) }

    /** GZip + Kryo 직렬화 조합 */
    val GzipKryo by lazy { RedisBinarySerializer(BinarySerializers.GZipKryo) }

    /** LZ4 + Kryo 직렬화 조합 */
    val LZ4Kryo by lazy { RedisBinarySerializer(BinarySerializers.LZ4Kryo) }

    /** Snappy + Kryo 직렬화 조합 */
    val SnappyKryo by lazy { RedisBinarySerializer(BinarySerializers.SnappyKryo) }

    /** Zstd + Kryo 직렬화 조합 */
    val ZstdKryo by lazy { RedisBinarySerializer(BinarySerializers.ZstdKryo) }

    /** GZip + Fory 직렬화 조합 */
    val GzipFory by lazy { RedisBinarySerializer(BinarySerializers.GZipFory) }

    /** LZ4 + Fory 직렬화 조합 */
    val LZ4Fory by lazy { RedisBinarySerializer(BinarySerializers.LZ4Fory) }

    /** Snappy + Fory 직렬화 조합 */
    val SnappyFory by lazy { RedisBinarySerializer(BinarySerializers.SnappyFory) }

    /** Zstd + Fory 직렬화 조합 */
    val ZstdFory by lazy { RedisBinarySerializer(BinarySerializers.ZstdFory) }

}
