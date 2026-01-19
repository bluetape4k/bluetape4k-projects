package io.bluetape4k.redis.spring.serializer

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.logging.KLogging
import org.springframework.data.redis.serializer.RedisSerializer

/**
 * Spring Data Redis 에서 사용할 수 있는 Binary Serializer 입니다.
 *
 * ```
 * val redisTemplate = RedisTemplate<String, Any>()
 * redisTemplate.keySerializer = StringRedisSerializer()
 * redisTemplate.valueSerializer = RedisBinarySerializers.LZ4Kryo
 *
 * redisTemplate.opsForValue().set("key", TestBean(1, "description"))  // TestBean 객체는 Kryo로 직렬화되고, LZ4 압축된 상태로 저장됩니다.
 * ```
 *
 * @property serializer Binary Serializer
 * @see BinarySerializer
 */
class RedisBinarySerializer private constructor(
    private val serializer: BinarySerializer,
): RedisSerializer<Any> {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(bs: BinarySerializer): RedisBinarySerializer {
            return RedisBinarySerializer(bs)
        }
    }

    override fun serialize(t: Any?): ByteArray? {
        return t?.let { serializer.serialize(it) }
    }

    override fun deserialize(bytes: ByteArray?): Any? {
        return serializer.deserialize(bytes)
    }
}
