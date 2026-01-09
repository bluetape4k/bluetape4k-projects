package io.bluetape4k.redis.redisson.nearcache

import io.bluetape4k.codec.Base58
import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.junit5.awaitility.suspendUntil
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.RedissonCodecs
import io.bluetape4k.testcontainers.storage.RedisServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.redisson.api.RMapCache
import org.redisson.api.options.LocalCachedMapOptions
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class RedissonNearCacheTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5

        private val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        private val redisson by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson(redis.url)
        }

        private val redisson1 by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson(redis.url)
        }
        private val redisson2 by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson(redis.url)
        }

        private fun randomName(): String = "nearcache-" + Base58.randomString(8)
        private fun randomValue(): String = Fakers.randomString(1024, 2048)
    }

    private val cacheName = randomName()
    private val options by lazy {
        LocalCachedMapOptions.name<String, Any>(cacheName)
            .cacheSize(100_000)
            .cacheProvider(LocalCachedMapOptions.CacheProvider.REDISSON)
            .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.SOFT)
            .timeToLive(1.seconds.toJavaDuration())
            .maxIdle(2.seconds.toJavaDuration())
            .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR)
            .syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
            .codec(RedissonCodecs.LZ4Fory)                                    // Codec 적용
    }

    private lateinit var nearCache1: RedissonNearCache<String, Any>
    private lateinit var nearCache2: RedissonNearCache<String, Any>
    private lateinit var backCache: RMapCache<String, Any>

    @BeforeAll
    fun beforeAll() {
        nearCache1 = RedissonNearCache(redisson1, options)
        nearCache2 = RedissonNearCache(redisson2, options)
        backCache = redisson.getMapCache(cacheName, RedissonCodecs.LZ4Fory)  // Codec 적용
    }

    @Nested
    inner class Sync {

        @RepeatedTest(REPEAT_SIZE)
        fun `nearCache1 에 cache item을 추가하면 nearCache2에 추가됩니다`() {
            val keyToAdd = randomName()
            val valueToAdd = randomName()

            log.debug { "near cache1: put key=$keyToAdd" }
            nearCache1.fastPut(keyToAdd, valueToAdd)

            await atMost 1.seconds.toJavaDuration() until { nearCache2.containsKey(keyToAdd) }

            nearCache2[keyToAdd] shouldBeEqualTo valueToAdd
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `nearCache1 에 cache item을 삭제하면 nearCache2에 삭제됩니다`() {
            val keyToRemove = randomName()
            val valueToRemove = randomName()

            log.debug { "near cache1: put key=$keyToRemove" }
            nearCache1.fastPut(keyToRemove, valueToRemove)

            await atMost 1.seconds.toJavaDuration() until { nearCache2.containsKey(keyToRemove) }
            nearCache2[keyToRemove] shouldBeEqualTo valueToRemove

            nearCache1.fastRemove(keyToRemove) shouldBeEqualTo 1

            await atMost 1.seconds.toJavaDuration() until { nearCache2.containsKey(keyToRemove).not() }
            nearCache2[keyToRemove].shouldBeNull()
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `backCache 에 캐시를 추가하면, near cache 들에게 반영된다`() {
            val key = randomName()
            val value = randomValue()

            // 기존 NearCache의 FrontCache에는 key가 존재하지 않는다
            nearCache1.containsKey(key).shouldBeFalse()
            nearCache2.containsKey(key).shouldBeFalse()

            // BackCache에 key를 추가한다
            log.debug { "put cache item to back cache. key=$key" }
            backCache.fastPut(key, value).shouldBeTrue()

            await atMost 1.seconds.toJavaDuration() until {
                nearCache1.containsKey(key)
            }
            // NearCache 들에게 신규 아이템이 반영된다.
            nearCache1.containsKey(key).shouldBeTrue()
            nearCache2.containsKey(key).shouldBeTrue()

            // BackCache에 key 를 삭제한다
            log.debug { "remove cache item from back cache. key=$key" }
            backCache.fastRemove(key) shouldBeEqualTo 1

            await atMost 1.seconds.toJavaDuration() until {
                nearCache1.containsKey(key).not()
            }

            // NearCache 들에게 삭제가 반영된다.
            nearCache1.containsKey(key).shouldBeFalse()
            nearCache2.containsKey(key).shouldBeFalse()
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `frontCache에 expiration을 설정하면, 해당 시간이 지나면 자동으로 삭제된다`() {
            val key = randomName()
            val value = randomValue()

            nearCache1.fastPut(key, value).shouldBeTrue()

            nearCache1.expire(Duration.ofSeconds(1))
            nearCache2.expire(Duration.ofSeconds(1))

            Thread.sleep(500)

            nearCache2.containsKey(key).shouldBeTrue()

            Thread.sleep(500)

            await atMost 3.seconds.toJavaDuration() until {
                nearCache2.containsKey(key).not()
            }

            // 1초가 지나서 expire 되었다.
            nearCache1.containsKey(key).shouldBeFalse()
        }
    }

    @Nested
    inner class Async {

        @RepeatedTest(REPEAT_SIZE)
        fun `nearCache1 에 cache item을 추가하면 nearCache2에 추가됩니다`() = runTest {
            val keyToAdd = randomName()
            val valueToAdd = randomName()

            log.debug { "near cache1: put key=$keyToAdd" }
            nearCache1.fastPutIfAbsentAsync(keyToAdd, valueToAdd).suspendAwait()

            await atMost 1.seconds.toJavaDuration() suspendUntil {
                nearCache2.containsKeyAsync(keyToAdd).suspendAwait()
            }

            nearCache2.getAsync(keyToAdd).suspendAwait() shouldBeEqualTo valueToAdd
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `nearCache1 에 cache item을 삭제하면 nearCache2에 삭제됩니다`() = runTest {
            val keyToRemove = randomName()
            val valueToRemove = randomName()

            log.debug { "near cache1: put key=$keyToRemove" }
            nearCache1.fastPutIfAbsentAsync(keyToRemove, valueToRemove).suspendAwait()

            await atMost 1.seconds.toJavaDuration() suspendUntil {
                nearCache2.containsKeyAsync(keyToRemove).suspendAwait()
            }
            nearCache2.getAsync(keyToRemove).suspendAwait() shouldBeEqualTo valueToRemove

            nearCache1.fastRemoveAsync(keyToRemove).suspendAwait() shouldBeEqualTo 1

            await atMost 1.seconds.toJavaDuration() suspendUntil {
                nearCache2.containsKeyAsync(keyToRemove).suspendAwait().not()
            }
            nearCache2.getAsync(keyToRemove).suspendAwait().shouldBeNull()
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `backCache 에 캐시를 추가하면, near cache 들에게 반영된다`() = runTest {
            val key = randomName()
            val value = randomValue()

            // 기존 NearCache의 FrontCache에는 key가 존재하지 않는다
            nearCache1.containsKeyAsync(key).suspendAwait().shouldBeFalse()
            nearCache2.containsKeyAsync(key).suspendAwait().shouldBeFalse()

            // BackCache에 key를 추가한다
            log.debug { "put cache item to back cache. key=$key" }
            backCache.fastPutIfAbsentAsync(key, value).suspendAwait().shouldBeTrue()

            await atMost 5.seconds.toJavaDuration() suspendUntil {
                nearCache1.containsKeyAsync(key).suspendAwait()
            }
            // NearCache 들에게 신규 아이템이 반영된다.
            nearCache1.containsKeyAsync(key).suspendAwait().shouldBeTrue()
            nearCache2.containsKeyAsync(key).suspendAwait().shouldBeTrue()

            // BackCache에 key 를 삭제한다
            log.debug { "remove cache item from back cache. key=$key" }
            backCache.fastRemoveAsync(key).suspendAwait() shouldBeEqualTo 1

            await atMost 5.seconds.toJavaDuration() suspendUntil {
                nearCache1.containsKeyAsync(key).suspendAwait().not()
            }

            // NearCache 들에게 삭제가 반영된다.
            nearCache1.containsKeyAsync(key).suspendAwait().shouldBeFalse()
            nearCache2.containsKeyAsync(key).suspendAwait().shouldBeFalse()
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `frontCache에 expiration을 설정하면, 해당 시간이 지나면 자동으로 삭제된다`() = runTest {
            val key1 = randomName()
            val value1 = randomValue()

            val key2 = randomName()
            val value2 = randomValue()

            nearCache1.fastPutAsync(key1, value1).suspendAwait().shouldBeTrue()
            launch {
                nearCache1.expireAsync(1.seconds.toJavaDuration())
            }

            delay(500)

            nearCache2.containsKeyAsync(key1).suspendAwait().shouldBeTrue()

            delay(500)

            await atMost 3.seconds.toJavaDuration() suspendUntil {
                nearCache2.containsKeyAsync(key1).suspendAwait().not()
            }

            delay(100)
            // 1초가 지나서 expire 되었다.
            nearCache1.containsKeyAsync(key1).suspendAwait().shouldBeFalse()

            // key2 에 대해서 expire 를 설정하지 않았으므로, 삭제되지 않는다.
            // 
            nearCache2.fastPutAsync(key2, value2).suspendAwait().shouldBeTrue()
            launch {
                nearCache2.expireAsync(Duration.ofSeconds(1))
            }
            delay(1000)
            await atMost 3.seconds.toJavaDuration() suspendUntil {
                nearCache1.containsKeyAsync(key2).suspendAwait().not()
            }
            nearCache1.containsKeyAsync(key2).suspendAwait().shouldBeFalse()
        }
    }
}
