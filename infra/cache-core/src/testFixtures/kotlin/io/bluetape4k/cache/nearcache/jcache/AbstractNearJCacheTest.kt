package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.codec.Base58
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.output.OutputCapture
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.time.Duration.Companion.seconds

@OutputCapture
@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractNearJCacheTest {

    companion object: KLogging() {
        protected const val TEST_SIZE = 3

        protected val awaitTimeout = 5.seconds

        @JvmStatic
        fun randomKey(): String = TimebasedUuid.Epoch.nextIdAsString() + Base58.randomString(6)

        @JvmStatic
        protected fun randomValue(): String =
            Fakers.randomString(1024, 8192, true)
    }

    abstract val backCache: JCache<String, Any>

    open val nearCacheCfg1 = NearJCacheConfig<String, Any>()
    open val nearCacheCfg2 = NearJCacheConfig<String, Any>()

    protected open val nearJCache1: NearJCache<String, Any> by lazy { NearJCache(nearCacheCfg1, backCache) }
    protected open val nearJCache2: NearJCache<String, Any> by lazy { NearJCache(nearCacheCfg2, backCache) }

    @BeforeEach
    fun setup() {
        nearJCache1.clear()
        nearJCache2.clear()
        backCache.clear()
    }

    @Test
    fun `create near cache`() {
        val frontCacheName = "frontCache-" + randomKey()
        val nearCacheCfg = NearJCacheConfig<String, Any>(cacheName = frontCacheName)
        nearCacheCfg.cacheName shouldBeEqualTo frontCacheName
        val nearJCache = NearJCache(nearCacheCfg, backCache)
        nearJCache.frontCache.name shouldBeEqualTo frontCacheName
    }

    @RepeatedTest(TEST_SIZE)
    fun `front 에 값이 없으면, back cache에 있는 값을 read through 로 가져오기`() {
        val key = randomKey()
        val value = randomValue()

        nearJCache1[key].shouldBeNull()

        backCache.put(key, value)
        await atMost (awaitTimeout) until { nearJCache1.containsKey(key) && nearJCache2.containsKey(key) }

        // 이 것은 cache entry event listener 를 통해 backCache -> frontCache로 전달된다
        nearJCache1[key] shouldBeEqualTo value
        nearJCache2[key] shouldBeEqualTo value
    }

    // TODO: 실제 시나리오를 만들기 힘듬 (시점 차이) -> Mockk 로 대체해야 함
    @Disabled("시나리오 미비 -> Mockk 으로 대체해야 함")
    @RepeatedTest(TEST_SIZE)
    fun `getDeeply - front miss면 back cache에서 조회하고 front cache를 채운다`() {
        val key = randomKey()
        val value = randomValue()

        backCache.put(key, value)
        nearJCache1.clear()
        nearJCache2.clear()

        nearJCache1[key].shouldBeNull()
        nearJCache2[key].shouldBeNull()

        nearJCache1.getDeeply(key) shouldBeEqualTo value
        nearJCache1[key] shouldBeEqualTo value
        backCache[key] shouldBeEqualTo value
    }

    @RepeatedTest(TEST_SIZE)
    fun `front cache 에 cache entry를 추가하면 write through로 back cache에 추가된다`() {
        val key = randomKey()
        val value = randomValue()

        backCache.containsKey(key).shouldBeFalse()

        nearJCache1.put(key, value)
        await atMost (awaitTimeout) until { nearJCache2.containsKey(key) }

        backCache.get(key) shouldBeEqualTo value   // 이 것은 write through 로
        nearJCache2[key] shouldBeEqualTo value  // 이 것은 cache entry event listener 로 추가됨
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 삭제하면 write through 로 back cache도 삭제된다`() {
        val key = randomKey()
        val value = randomValue()

        backCache.containsKey(key).shouldBeFalse()

        nearJCache1.put(key, value)
        await atMost (awaitTimeout) until { nearJCache2.containsKey(key) }

        backCache.containsKey(key).shouldBeTrue()
        nearJCache2.containsKey(key).shouldBeTrue()

        nearJCache1.remove(key)
        await atMost (awaitTimeout) until { !nearJCache2.containsKey(key) }

        backCache.containsKey(key).shouldBeFalse()
        nearJCache2.containsKey(key).shouldBeFalse()
        nearJCache2[key].shouldBeNull()
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 update 하면 다른 캐시도 update 된다`() {
        val key = randomKey()
        val oldValue = randomValue()
        val newValue = randomValue()

        backCache.containsKey(key).shouldBeFalse()

        nearJCache1.put(key, oldValue)
        await atMost (awaitTimeout) until { nearJCache2.containsKey(key) }

        backCache.get(key) shouldBeEqualTo oldValue     // write through로 인해
        nearJCache2.containsKey(key).shouldBeTrue()
        nearJCache2[key] shouldBeEqualTo oldValue    // read through로 인해

        nearJCache1.replace(key, newValue)
        await atMost (awaitTimeout) until { nearJCache2[key] == newValue }

        backCache.get(key) shouldBeEqualTo newValue     // write through로 인해
        nearJCache2[key] shouldBeEqualTo newValue
    }

    @RepeatedTest(TEST_SIZE)
    fun `remote를 공유하는 nearCache 가 값을 공유합니다`() {
        val key1 = randomKey()
        val value1 = randomValue()
        val key2 = randomKey()
        val value2 = randomValue()

        nearJCache1.put(key1, value1)  // write through -> remote -> event -> event cache2
        nearJCache2.put(key2, value2)  // write through -> remote -> event -> event cache1
        await atMost (awaitTimeout) until { nearJCache1.containsKey(key2) && nearJCache2.containsKey(key1) }

        nearJCache1.getAll(key1, key2) shouldContainSame mapOf(key1 to value1, key2 to value2)
    }

    @RepeatedTest(TEST_SIZE)
    fun `containsKey - 캐시에 entry를 추가하면 다른 NearCache에도 추가된다`() {
        val key = randomKey()
        val value = randomValue()

        nearJCache1.containsKey(key).shouldBeFalse()
        nearJCache2.containsKey(key).shouldBeFalse()

        nearJCache1.put(key, value)    // write through -> remote -> event -> near cache2
        await atMost (awaitTimeout) until { nearJCache2.containsKey(key) }
        nearJCache1.containsKey(key).shouldBeTrue()
        nearJCache2.containsKey(key).shouldBeTrue()
    }

    @RepeatedTest(TEST_SIZE)
    fun `put - writeThrough 와 event 를 통해 다른 cache에도 적용된다`() {
        val key = randomKey()
        val value = randomValue()

        nearJCache1.containsKey(key).shouldBeFalse()
        nearJCache2.containsKey(key).shouldBeFalse()

        nearJCache1.put(key, value)    // write through -> backCache -> event -> nearCache2
        await atMost (awaitTimeout) until { nearJCache2.containsKey(key) }

        nearJCache1[key] shouldBeEqualTo value
        backCache.get(key) shouldBeEqualTo value
        nearJCache2[key] shouldBeEqualTo value
    }

    @RepeatedTest(TEST_SIZE)
    fun `putAll - 복수의 캐시를 저장하면 다른 cache에 모두 반영된다`() {
        val map = List(10) {
            randomKey() to randomValue()
        }.toMap()

        nearJCache1.putAll(map)
        await atMost (awaitTimeout) until { nearJCache2.getAll(*map.keys.toTypedArray()).size == map.size }

        map.keys.all { nearJCache1[it] != null }.shouldBeTrue()
        map.keys.all { nearJCache2[it] != null }.shouldBeTrue()

        nearJCache2.getAll(map.keys.toSet()) shouldContainSame map
    }

    @RepeatedTest(TEST_SIZE)
    fun `putIfAbsent - 값이 없는 경우에 추가합니다`() {
        val key = "key-1"
        val oldValue = randomValue()
        val newValue = randomValue()

        nearJCache1.put(key, oldValue)
        nearJCache1[key] shouldBeEqualTo oldValue

        await atMost (awaitTimeout) until { nearJCache2.containsKey(key) }

        // 이미 등록되어 있는 key 에 대해 저장되지 않는다
        nearJCache2.putIfAbsent(key, newValue).shouldBeFalse()

        nearJCache2[key] shouldBeEqualTo oldValue
        nearJCache2[key] shouldBeEqualTo oldValue
    }

    @RepeatedTest(TEST_SIZE)
    fun `putIfAbsent - 값이 없는 경우`() {
        // 등록되지 않는 key2 에 대해서 새로 등록한다 -> backCache에 등록되어 다른 nearCache에 전달되어야 합니다.
        val key = "not-exist-key"
        val value = randomValue()

        nearJCache2.putIfAbsent(key, value).shouldBeTrue()
        nearJCache2[key] shouldBeEqualTo value

        await atMost (awaitTimeout) until { nearJCache1.containsKey(key) }

        backCache[key] shouldBeEqualTo value
        nearJCache1[key] shouldBeEqualTo value
    }

    @RepeatedTest(TEST_SIZE)
    fun `remove - cache entry를 삭제하면 모든 near cache에서 삭제됩니다`() {
        val key = randomKey()
        val value = randomValue()

        nearJCache1.put(key, value)
        await atMost (awaitTimeout) until { nearJCache2.containsKey(key) }

        nearJCache1.containsKey(key).shouldBeTrue()
        nearJCache2.containsKey(key).shouldBeTrue()
        nearJCache2[key] shouldBeEqualTo value

        nearJCache2.remove(key)
        await atMost (awaitTimeout) untilNull { nearJCache1[key] }

        nearJCache1.containsKey(key).shouldBeFalse()
        nearJCache2.containsKey(key).shouldBeFalse()
        nearJCache1[key].shouldBeNull()
        nearJCache2[key].shouldBeNull()
    }

    @RepeatedTest(TEST_SIZE)
    fun `remove with value - cache entry를 삭제하면 모든 near cache에서 삭제됩니다`() {
        val key = randomKey()
        val oldValue = randomValue()
        val newValue = randomValue()

        nearJCache1.put(key, newValue)
        await atMost (awaitTimeout) until { nearJCache2[key] == newValue }

        nearJCache2.put(key, oldValue)
        await atMost (awaitTimeout) until { nearJCache1[key] == oldValue }

        // nearCache2에서 update 한 것이 반영되었다
        nearJCache1.remove(key, oldValue).shouldBeTrue()
        await atMost (awaitTimeout) untilNull { nearJCache2[key] }

        nearJCache1.containsKey(key).shouldBeFalse()
        nearJCache2.containsKey(key).shouldBeFalse()

        nearJCache1.put(key, newValue)
        await atMost (awaitTimeout) until { nearJCache2[key] == newValue }

        nearJCache2.put(key, oldValue)
        await atMost (awaitTimeout) until { nearJCache1[key] == oldValue }

        // 마지막 Layer의 Cache 값이 Update 되어서 oldValue를 가진다.
        nearJCache1.remove(key, oldValue).shouldBeTrue()
        await atMost (awaitTimeout) untilNull { nearJCache2[key] }

        nearJCache1[key].shouldBeNull()
        nearJCache2[key].shouldBeNull()

        // 다른 값으로 삭제가 실패할 경우에는 값이 존재한다
        nearJCache1.put(key, oldValue)
        nearJCache1.remove(key, newValue).shouldBeFalse()
        await atMost (awaitTimeout) until { nearJCache2[key] == oldValue }

        nearJCache1[key] shouldBeEqualTo oldValue
        nearJCache2[key] shouldBeEqualTo oldValue
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and remove - getAndRemove 시 모든 캐시에서 제거된다`() {
        val key = randomKey()
        val value = randomValue()
        val value2 = randomValue()

        nearJCache1.put(key, value)
        await atMost (awaitTimeout) until { nearJCache2[key] == value }

        nearJCache1.getAndRemove(key) shouldBeEqualTo value
        await atMost (awaitTimeout) until { nearJCache1[key] == null && nearJCache2[key] == null }

        nearJCache1.containsKey(key).shouldBeFalse()
        nearJCache2.containsKey(key).shouldBeFalse()

        nearJCache2.put(key, value)
        await atMost (awaitTimeout) until { nearJCache1[key] == value }

        nearJCache1.getAndRemove(key) shouldBeEqualTo value
        await atMost (awaitTimeout) until { nearJCache2[key] == null }

        nearJCache1.containsKey(key).shouldBeFalse()
        nearJCache2.containsKey(key).shouldBeFalse()

        nearJCache1.put(key, value)
        nearJCache2.put(key, value)
        await atMost (awaitTimeout) until { nearJCache1[key] == value }

        nearJCache1.getAndRemove(key) shouldBeEqualTo value
        await atMost (awaitTimeout) until { nearJCache2[key] == null }

        nearJCache1.containsKey(key).shouldBeFalse()
        nearJCache2.containsKey(key).shouldBeFalse()

        // 마지막 Layer의 변경이 전파된다.
        nearJCache1.put(key, value)
        await atMost (awaitTimeout) until { nearJCache2[key] == value }

        // BackCache가 변경되면 모든 NearCache에 전파됩니다
        backCache.put(key, value2)
        await atMost (awaitTimeout) until { nearJCache1[key] == value2 && nearJCache2[key] == value2 }

        nearJCache1[key] shouldBeEqualTo value2
        nearJCache2[key] shouldBeEqualTo value2

        nearJCache1.getAndRemove(key) shouldBeEqualTo value2
        await atMost (awaitTimeout) until { nearJCache2[key] == null }

        nearJCache2.containsKey(key).shouldBeFalse()
        backCache.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `replace old value - 모든 캐시에 적용되어야 합니다`() {
        val key = randomKey()
        val oldValue = randomValue()
        val newValue = randomValue()

        nearJCache2.put(key, oldValue)
        await atMost (awaitTimeout) until { nearJCache1[key] == oldValue }

        nearJCache1.replace(key, oldValue, newValue).shouldBeTrue()
        await atMost (awaitTimeout) until { nearJCache2[key] == newValue }

        nearJCache1[key] shouldBeEqualTo newValue
        nearJCache2[key] shouldBeEqualTo newValue
        backCache.get(key) shouldBeEqualTo newValue

        // 이미 newValue를 가진다
        nearJCache2.replace(key, oldValue, newValue).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `repalce - value를 변경하면 모든 캐시에 적용되어야 한다`() {
        val key = randomKey()
        val oldValue = randomValue()
        val newValue = randomValue()

        nearJCache1.put(key, oldValue)
        await atMost (awaitTimeout) until { nearJCache2.containsKey(key) }

        nearJCache2.replace(key, newValue).shouldBeTrue()
        await atMost (awaitTimeout) until { nearJCache1[key] == newValue }

        nearJCache1[key] shouldBeEqualTo newValue

        nearJCache1.remove(key)
        await atMost (awaitTimeout) until { nearJCache2[key] == null }

        nearJCache2.replace(key, newValue).shouldBeFalse()
        nearJCache1.replace(key, newValue).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and replace - 새로운 값으로 대체하고, 기존 값을 반환`() {
        val key = randomKey()
        val oldValue = randomKey()
        val oldValue2 = randomValue()
        val newValue = randomValue()

        // 기존에 key가 없으므로 replace 하지 못한다
        nearJCache1.getAndReplace(key, oldValue).shouldBeNull()

        nearJCache1.put(key, oldValue)
        await atMost (awaitTimeout) until { nearJCache2[key] == oldValue }

        // 이제 key가 있으니 oldValue를 반환하고, newValue를 저장한다
        nearJCache2.getAndReplace(key, newValue) shouldBeEqualTo oldValue
        await atMost (awaitTimeout) until { nearJCache1[key] == newValue }

        nearJCache1[key] shouldBeEqualTo newValue
        nearJCache2[key] shouldBeEqualTo newValue

        nearJCache1.clear()
        nearJCache2.clear()
        await until { nearJCache1.count() == 0 && nearJCache2.count() == 0 }

        nearJCache1.put(key, oldValue)
        await atMost (awaitTimeout) until { nearJCache2[key] == oldValue }
        nearJCache2.put(key, oldValue2)
        await atMost (awaitTimeout) until { nearJCache1[key] == oldValue2 }

        nearJCache1.getAndReplace(key, newValue) shouldBeEqualTo oldValue2
        await atMost (awaitTimeout) until { nearJCache2[key] == newValue }
        nearJCache1[key] shouldBeEqualTo newValue
        nearJCache2[key] shouldBeEqualTo newValue

        // key가 존재하지 않으므로 replace도 하지 않는다
        nearJCache1.remove(key)
        await atMost (awaitTimeout) until { nearJCache2[key] == null }
        nearJCache2.getAndReplace(key, newValue).shouldBeNull()
        nearJCache1.containsKey(key).shouldBeFalse()
        nearJCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll with keys - 모든 캐시를 삭제하면 다른 캐시에도 반영된다`() {
        val key1 = randomKey()
        val value1 = randomValue()
        val key2 = randomKey()
        val value2 = randomValue()

        nearJCache1.put(key1, value1)
        nearJCache2.put(key2, value2)
        await atMost (awaitTimeout) until { nearJCache1.containsKey(key2) && nearJCache2.containsKey(key1) }

        nearJCache1.removeAll(key1, key2)
        await atMost (awaitTimeout) until { nearJCache2[key1] == null && nearJCache2[key2] == null }

        nearJCache1.containsKey(key1).shouldBeFalse()
        nearJCache1.containsKey(key2).shouldBeFalse()
        nearJCache2.containsKey(key1).shouldBeFalse()
        nearJCache2.containsKey(key2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    open fun `removeAll - 모든 캐시를 삭제하면 다른 캐시에도 반영된다`() {
        val key1 = randomKey()
        val value1 = randomValue()
        val key2 = randomKey()
        val value2 = randomValue()

        nearJCache1.put(key1, value1)
        nearJCache2.put(key2, value2)
        await atMost (awaitTimeout) until { nearJCache1.containsKey(key2) && nearJCache2.containsKey(key1) }

        nearJCache1.removeAll()
        await atMost (awaitTimeout) until { nearJCache2[key1] == null && nearJCache2[key2] == null }

        nearJCache1.containsKey(key1).shouldBeFalse()
        nearJCache1.containsKey(key2).shouldBeFalse()
        nearJCache2.containsKey(key1).shouldBeFalse()
        nearJCache2.containsKey(key2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `clear - cache를 clear 합니다 - front cache만 clear 될 뿐 back cache는 유지됩니다`() {
        val key1 = randomKey()
        val value1 = randomValue()
        val key2 = randomKey()
        val value2 = randomValue()

        nearJCache1.put(key1, value1)
        await atMost (awaitTimeout) until { nearJCache2.containsKey(key1) }

        nearJCache2.put(key2, value2)
        await atMost (awaitTimeout) until { nearJCache1.containsKey(key2) }

        // 로컬 캐시만 삭제됩니다. backCache는 삭제되지 않습니다.
        nearJCache1.clear()

        // frontCache에서 containsKey 를 조회합니다.
        nearJCache1.containsKey(key1).shouldBeFalse()
        nearJCache1.containsKey(key2).shouldBeFalse()

        // 다른 캐시에는 전파되지 않습니다
        nearJCache2.containsKey(key1).shouldBeTrue()
        nearJCache2.containsKey(key2).shouldBeTrue()
    }

    @RepeatedTest(TEST_SIZE)
    fun `clearBackCache - back cache를 삭제하지만 전파는 되지 않습니다`() {
        val key1 = randomKey()
        val value1 = randomValue()
        val key2 = randomKey()
        val value2 = randomValue()

        nearJCache1.put(key1, value1)
        nearJCache2.put(key2, value2)
        await atMost (awaitTimeout) until { nearJCache1.containsKey(key2) && nearJCache2.containsKey(key1) }

        nearJCache1.clearAllCache()

        // front & back cache 모두 삭제한다
        nearJCache1.containsKey(key1).shouldBeFalse()
        nearJCache1.containsKey(key2).shouldBeFalse()
        backCache.containsKey(key1).shouldBeFalse()
        backCache.containsKey(key2).shouldBeFalse()

        // 다른 nearCache에는 전파되지 않습니다
        nearJCache2.containsKey(key1).shouldBeTrue()
        nearJCache2.containsKey(key2).shouldBeTrue()
    }

    // ─────────────────────────────────────────────
    // 동시성 테스트
    // ─────────────────────────────────────────────

    @Test
    fun `MultithreadingTester - 동시 put과 get이 안전하다`() {
        val keys = (1..100).map { randomKey() }
        val value = randomValue()

        // 먼저 데이터 넣기
        keys.forEach { nearJCache1.put(it, value) }
        await until { keys.all { nearJCache2.containsKey(it) } }

        MultithreadingTester()
            .workers(8)
            .rounds(4)
            .add {
                val key = keys.random()
                nearJCache1[key] shouldBeEqualTo value
            }
            .add {
                val key = randomKey()
                nearJCache2.put(key, value)
            }
            .run()
    }

    @Test
    fun `MultithreadingTester - 동시 put과 remove가 안전하다`() {
        MultithreadingTester()
            .workers(8)
            .rounds(4)
            .add {
                val key = randomKey()
                nearJCache1.put(key, randomValue())
                nearJCache1.remove(key)
                // 비동기 이벤트 전파로 인해 즉시 null이 아닐 수 있으므로, 예외 없이 실행됨만 검증
            }
            .run()
    }

    @Test
    fun `StructuredTaskScopeTester - 병렬 put-get-remove 사이클`() {
        StructuredTaskScopeTester()
            .rounds(32)
            .add {
                val key = randomKey()
                val value = randomValue()
                nearJCache1.put(key, value)
                nearJCache1[key] // get 호출 (비동기 전파로 값이 다를 수 있음)
                nearJCache1.remove(key)
                // 예외 없이 사이클이 완료됨을 검증
            }
            .run()
    }

    @Test
    fun `StructuredTaskScopeTester - 동시 putIfAbsent 경합`() {
        val sharedKey = randomKey()
        val value = randomValue()

        // 여러 태스크가 동시에 putIfAbsent 시도 — 하나만 성공해야 함
        StructuredTaskScopeTester()
            .rounds(32)
            .add {
                nearJCache1.putIfAbsent(sharedKey, value)
            }
            .run()

        nearJCache1[sharedKey] shouldBeEqualTo value
    }
}
