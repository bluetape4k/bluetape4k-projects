package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.codec.Base58
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.junit.jupiter.api.Test
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.codec.CompositeCodec
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val intCodec = CompositeCodec(
    RedissonCodecs.String,
    RedissonCodecs.Int,
    RedissonCodecs.Int,
)

private val doubleCodec = CompositeCodec(
    RedissonCodecs.String,
    RedissonCodecs.Double,
    RedissonCodecs.Double,
)

/**
 * Redisson [RLocalCachedMap] 은 NearCache 와 같은 역할을 수행한다.
 *
 * 참고: [Redisson 7.-Distributed-collections](https://github.com/redisson/redisson/wiki/7.-Distributed-collections)
 *
 * 숫자 증분 예제는 String key와 숫자 value를 같은 [CompositeCodec]으로 구성한다.
 * Redisson의 `addAndGetAsync`는 Redis hash field에 `HINCRBYFLOAT`를 실행하므로,
 * Int와 Double map을 섞지 않고 각 타입에 맞는 value codec을 사용해야 한다.
 */
class LocalCachedMapExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `simple local cached map`() = runSuspendIO(timeout = 60.seconds) {
        val cachedMapName = "local:" + Base58.randomString(8)

        val options = LocalCachedMapOptions.name<String, Int>(cachedMapName)
            .cacheSize(10000)
            .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
            .maxIdle(10.seconds.toJavaDuration())
            .timeToLive(60.seconds.toJavaDuration())
            .retryAttempts(3)
            .retryDelay { attempt ->
                log.debug { "Retry attempt: $attempt" }
                Duration.ofMillis(attempt * 10L + 10)
            }  // 재시도 간격
            .timeout(Duration.ofSeconds(10))
            .codec(intCodec)


        val cachedMap: RLocalCachedMap<String, Int> = redisson.getLocalCachedMap(options)

        // NOTE: fastPutAsync 의 결과는 new insert 인 경우는 true, update 는 false 를 반환한다.
        awaitRedis(cachedMap.fastPutAsync("a", 1)).shouldBeTrue()
        awaitRedis(cachedMap.fastPutAsync("b", 2)).shouldBeTrue()
        awaitRedis(cachedMap.fastPutAsync("c", 3)).shouldBeTrue()

        awaitRedis(cachedMap.containsKeyAsync("a")).shouldBeTrue()

        awaitRedis(cachedMap.getAsync("c")) shouldBeEqualTo 3

        // 저장된 Int 형태의 저장 크기
        // valueSizeAsync 결과는 codec에 따라 저장된 Int 바이트 크기를 반환한다.

        val keys = setOf("a", "b", "c")

        val mapSlice = awaitRedis(cachedMap.getAllAsync(keys))
        mapSlice shouldBeEqualTo mapOf("a" to 1, "b" to 2, "c" to 3)

        awaitRedis(cachedMap.readAllKeySetAsync()) shouldContainSame setOf("a", "b", "c")
        awaitRedis(cachedMap.readAllValuesAsync()) shouldContainSame listOf(1, 2, 3)
        awaitRedis(cachedMap.readAllEntrySetAsync())
            .associate { it.key to it.value } shouldContainSame mapOf("a" to 1, "b" to 2, "c" to 3)

        // 신규 Item일 경우 true, Update 시에는 false 를 반환한다
        awaitRedis(cachedMap.fastPutAsync("a", 100)).shouldBeFalse()
        awaitRedis(cachedMap.fastPutAsync("d", 33)).shouldBeTrue()

        // 삭제 시에는 삭제된 갯수를 반환
        awaitRedis(cachedMap.fastRemoveAsync("b")) shouldBeEqualTo 1L

        // Remote 에 저장되었나 본다
        val backendMap = redisson.getMap<String, Int>(cachedMapName, intCodec)
        awaitRedis(backendMap.containsKeyAsync("a")).shouldBeTrue()
    }

    @Test
    fun `empty Int key is initialized by addAndGetAsync`() = runSuspendIO(timeout = 60.seconds) {
        val name = randomName()
        val cachedMap: RLocalCachedMap<String, Int> = redisson.getLocalCachedMap(
            LocalCachedMapOptions.name<String, Int>(name).codec(intCodec)
        )

        val first = awaitRedis(cachedMap.addAndGetAsync("count", 32))
        val second = awaitRedis(cachedMap.addAndGetAsync("count", 10))
        val backendMap: RMap<String, Int> = redisson.getMap(name, intCodec)

        first shouldBeEqualTo 32
        second shouldBeEqualTo 42
        awaitRedis(backendMap.getAsync("count")) shouldBeEqualTo 42
    }

    @Test
    fun `empty Double key is initialized by HINCRBYFLOAT`() = runSuspendIO(timeout = 60.seconds) {
        val name = randomName()
        val cachedMap: RLocalCachedMap<String, Double> = redisson.getLocalCachedMap(
            LocalCachedMapOptions.name<String, Double>(name).codec(doubleCodec)
        )
        val backendMap: RMap<String, Double> = redisson.getMap(name, doubleCodec)

        awaitRedis(cachedMap.addAndGetAsync("ratio", 0.25)) shouldBeEqualTo 0.25
        awaitRedis(backendMap.getAsync("ratio")) shouldBeEqualTo 0.25
    }
}
