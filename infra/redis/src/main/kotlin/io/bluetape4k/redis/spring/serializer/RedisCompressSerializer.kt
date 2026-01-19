package io.bluetape4k.redis.spring.serializer

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KLogging
import org.springframework.data.redis.serializer.RedisSerializer

/**
 * Spring Data Redis 에서 사용할 수 있는 [RedisSerializer]`<ByteArray>` 입니다.
 * [compressor]를 이용하여 데이터를 압축/복원합니다.
 *
 * ```
 * val redisTemplate = RedisTemplate<String, ByteArray>()
 * redisTemplate.keySerializer = StringRedisSerializer()
 * redisTemplate.valueSerializer = RedisCompressSerializer(Compressors.LZ4)
 *
 * redisTemplate.opsForValue().set("key", "value".toByteArray())  // "value" 는 LZ4 압축된 상태로 저장됩니다.
 * ```
 *
 * @property compressor Compressor
 */
class RedisCompressSerializer private constructor(
    private val compressor: Compressor,
): RedisSerializer<ByteArray> {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(compressor: Compressor = Compressors.LZ4): RedisCompressSerializer {
            return RedisCompressSerializer(compressor)
        }
    }

    override fun serialize(value: ByteArray?): ByteArray? {
        return value?.let { compressor.compress(it) }
    }

    override fun deserialize(bytes: ByteArray?): ByteArray? {
        return bytes?.let { compressor.decompress(it) }
    }
}
