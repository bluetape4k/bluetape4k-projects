package io.bluetape4k.spring.redis.serializer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.data.redis.serializer.RedisSerializer

class RedisSerializationContextSupportTest: AbstractRedisSerializerTest() {

    // --- DSL 방식 ---

    @Test
    fun `redisSerializationContext DSL로 컨텍스트를 생성한다`() {
        val context = redisSerializationContext {
            key(RedisSerializer.string())
            value(RedisBinarySerializers.LZ4Fory)
            hashKey(RedisSerializer.string())
            hashValue(RedisBinarySerializers.LZ4Fory)
        }

        context.shouldNotBeNull()
        context.keySerializationPair.shouldNotBeNull()
        context.valueSerializationPair.shouldNotBeNull()
    }

    @Test
    fun `redisSerializationContext DSL로 생성한 컨텍스트로 키를 직렬화한다`() {
        val context = redisSerializationContext {
            key(RedisSerializer.string())
            value(RedisBinarySerializers.LZ4Fory)
            hashKey(RedisSerializer.string())
            hashValue(RedisBinarySerializers.LZ4Fory)
        }

        val keyPair = context.keySerializationPair
        keyPair.read(keyPair.write("mykey")) shouldBeEqualTo "mykey"
    }

    @Test
    fun `redisSerializationContext DSL로 생성한 컨텍스트로 값을 직렬화한다`() {
        val context = redisSerializationContext {
            key(RedisSerializer.string())
            value(RedisBinarySerializers.LZ4Fory)
            hashKey(RedisSerializer.string())
            hashValue(RedisBinarySerializers.LZ4Fory)
        }

        val sample = newTestData()
        val valuePair = context.valueSerializationPair
        valuePair.read(valuePair.write(sample)) shouldBeEqualTo sample
    }

    @Test
    fun `defaultSerializer를 지정해 컨텍스트를 생성한다`() {
        val context = redisSerializationContext(
            defaultSerializer = RedisSerializer.string()
        ) {
            key(RedisSerializer.string())
            value(RedisBinarySerializers.LZ4Fory)
            hashKey(RedisSerializer.string())
            hashValue(RedisBinarySerializers.LZ4Fory)
        }

        context.shouldNotBeNull()
        val sample = newTestData()
        val valuePair = context.valueSerializationPair
        valuePair.read(valuePair.write(sample)) shouldBeEqualTo sample
    }

    // --- 편의 함수 방식 ---

    @Test
    fun `redisSerializationContextOf로 키와 값 serializer를 지정해 컨텍스트를 생성한다`() {
        val context = redisSerializationContextOf(
            keySerializer = RedisSerializer.string(),
            valueSerializer = RedisBinarySerializers.LZ4Fory,
        )

        context.shouldNotBeNull()

        val keyPair = context.keySerializationPair
        keyPair.read(keyPair.write("hello")) shouldBeEqualTo "hello"

        val sample = newTestData()
        val valuePair = context.valueSerializationPair
        valuePair.read(valuePair.write(sample)) shouldBeEqualTo sample
    }

    @Test
    fun `String 키 편의 함수로 컨텍스트를 생성한다`() {
        val context = redisSerializationContextOf(
            valueSerializer = RedisBinarySerializers.LZ4Fory,
        )

        context.shouldNotBeNull()

        val keyPair = context.keySerializationPair
        keyPair.read(keyPair.write("mykey")) shouldBeEqualTo "mykey"

        val sample = newTestData()
        val valuePair = context.valueSerializationPair
        valuePair.read(valuePair.write(sample)) shouldBeEqualTo sample
    }

    @Test
    fun `ZstdFory serializer로 컨텍스트를 생성한다`() {
        val context = redisSerializationContextOf(
            valueSerializer = RedisBinarySerializers.ZstdFory,
        )

        val sample = newTestData()
        val valuePair = context.valueSerializationPair
        valuePair.read(valuePair.write(sample)) shouldBeEqualTo sample
    }

    @Test
    fun `ZstdFastFory serializer로 컨텍스트를 생성한다`() {
        val context = redisSerializationContextOf(
            valueSerializer = RedisBinarySerializers.ZstdFastFory,
        )

        val sample = newTestData()
        val valuePair = context.valueSerializationPair
        valuePair.read(valuePair.write(sample)) shouldBeEqualTo sample
    }
}
