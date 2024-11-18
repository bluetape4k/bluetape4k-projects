package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
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
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@OutputCapture
@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractNearCacheTest {

    companion object: KLogging() {
        protected const val TEST_SIZE = 5

        @JvmStatic
        fun randomKey(): String = TimebasedUuid.nextBase62String()

        @JvmStatic
        protected fun randomValue(): String =
            Fakers.randomString(1024, 8192, true)
    }

    abstract val backCache: JCache<String, Any>

    open val nearCacheCfg1 = NearCacheConfig<String, Any>()
    open val nearCacheCfg2 = NearCacheConfig<String, Any>()

    protected open val nearCache1: NearCache<String, Any> by lazy { NearCache(nearCacheCfg1, backCache) }
    protected open val nearCache2: NearCache<String, Any> by lazy { NearCache(nearCacheCfg2, backCache) }

    @BeforeEach
    fun setup() {
        nearCache1.clear()
        nearCache1.clear()
        nearCache2.clear()
        backCache.clear()
    }

    @Test
    fun `create near cache`() {
        val frontCacheName = "frontCache-" + randomKey()
        val nearCacheCfg = NearCacheConfig<String, Any>(frontCacheName = frontCacheName)
        nearCacheCfg.frontCacheName shouldBeEqualTo frontCacheName
        val nearCache = NearCache(nearCacheCfg, backCache)
        nearCache.frontCache.name shouldBeEqualTo frontCacheName
    }

    @RepeatedTest(TEST_SIZE)
    fun `front 에 값이 없으면, back cache에 있는 값을 read through 로 가져오기`() {
        val key = randomKey()
        val value = randomValue()

        nearCache1.get(key).shouldBeNull()

        backCache.put(key, value)
        await until { nearCache1.containsKey(key) && nearCache2.containsKey(key) }

        // 이 것은 cache entry event listener 를 통해 backCache -> frontCache로 전달된다
        nearCache1.get(key) shouldBeEqualTo value
        nearCache2.get(key) shouldBeEqualTo value
    }

    @RepeatedTest(TEST_SIZE)
    fun `front cache 에 cache entry를 추가하면 write through로 back cache에 추가된다`() {
        val key = randomKey()
        val value = randomValue()

        backCache.containsKey(key).shouldBeFalse()

        nearCache1.put(key, value)
        await until { nearCache2.containsKey(key) }

        backCache.get(key) shouldBeEqualTo value   // 이 것은 write through 로
        nearCache2.get(key) shouldBeEqualTo value  // 이 것은 cache entry event listener 로 추가됨
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 삭제하면 write through 로 back cache도 삭제된다`() {
        val key = randomKey()
        val value = randomValue()

        backCache.containsKey(key).shouldBeFalse()

        nearCache1.put(key, value)
        await until { nearCache2.containsKey(key) }

        backCache.containsKey(key).shouldBeTrue()
        nearCache2.containsKey(key).shouldBeTrue()

        nearCache1.remove(key)
        await until { !nearCache2.containsKey(key) }

        backCache.containsKey(key).shouldBeFalse()
        nearCache2.containsKey(key).shouldBeFalse()
        nearCache2.get(key).shouldBeNull()
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 update 하면 다른 캐시도 update 된다`() {
        val key = randomKey()
        val oldValue = randomValue()
        val newValue = randomValue()

        backCache.containsKey(key).shouldBeFalse()

        nearCache1.put(key, oldValue)
        await until { nearCache2.containsKey(key) }

        backCache.get(key) shouldBeEqualTo oldValue     // write through로 인해
        nearCache2.containsKey(key).shouldBeTrue()
        nearCache2.get(key) shouldBeEqualTo oldValue    // read through로 인해

        nearCache1.replace(key, newValue)
        await until { nearCache2.get(key) == newValue }

        backCache.get(key) shouldBeEqualTo newValue     // write through로 인해
        nearCache2.get(key) shouldBeEqualTo newValue
    }

    @RepeatedTest(TEST_SIZE)
    fun `remote를 공유하는 nearCache 가 값을 공유합니다`() {
        val key1 = randomKey()
        val value1 = randomValue()
        val key2 = randomKey()
        val value2 = randomValue()

        nearCache1.put(key1, value1)  // write through -> remote -> event -> event cache2
        nearCache2.put(key2, value2)  // write through -> remote -> event -> event cache1
        await until { nearCache1.containsKey(key2) && nearCache2.containsKey(key1) }

        nearCache1.getAll(key1, key2) shouldContainSame mapOf(key1 to value1, key2 to value2)
    }

    @RepeatedTest(TEST_SIZE)
    fun `containsKey - 캐시에 entry를 추가하면 다른 NearCache에도 추가된다`() {
        val key = randomKey()
        val value = randomValue()

        nearCache1.containsKey(key).shouldBeFalse()
        nearCache2.containsKey(key).shouldBeFalse()

        nearCache1.put(key, value)    // write through -> remote -> event -> near cache2
        await until { nearCache2.containsKey(key) }
        nearCache1.containsKey(key).shouldBeTrue()
        nearCache2.containsKey(key).shouldBeTrue()
    }

    @RepeatedTest(TEST_SIZE)
    fun `put - writeThrough 와 event 를 통해 다른 cache에도 적용된다`() {
        val key = randomKey()
        val value = randomValue()

        nearCache1.containsKey(key).shouldBeFalse()
        nearCache2.containsKey(key).shouldBeFalse()

        nearCache1.put(key, value)    // write through -> backCache -> event -> nearCache2
        await until { nearCache2.containsKey(key) }

        nearCache1.get(key) shouldBeEqualTo value
        backCache.get(key) shouldBeEqualTo value
        nearCache2.get(key) shouldBeEqualTo value
    }

    @RepeatedTest(TEST_SIZE)
    fun `putAll - 복수의 캐시를 저장하면 다른 cache에 모두 반영된다`() {
        val map = List(10) {
            randomKey() to randomValue()
        }.toMap()

        nearCache1.putAll(map)
        await until { nearCache2.getAll(*map.keys.toTypedArray()).size == map.size }

        map.keys.all { nearCache1.get(it) != null }.shouldBeTrue()
        map.keys.all { nearCache2.get(it) != null }.shouldBeTrue()

        nearCache2.getAll(map.keys.toSet()) shouldContainSame map
    }

    @RepeatedTest(TEST_SIZE)
    fun `putIfAbsent - 값이 없는 경우에 추가합니다`() {
        val key = "key-1"
        val oldValue = randomValue()
        val newValue = randomValue()

        nearCache1.put(key, oldValue)
        nearCache1[key] shouldBeEqualTo oldValue
        await until { nearCache2.containsKey(key) }

        // 이미 등록되어 있는 key 에 대해 저장되지 않는다
        nearCache2.putIfAbsent(key, newValue).shouldBeFalse()

        nearCache2[key] shouldBeEqualTo oldValue
        nearCache2[key] shouldBeEqualTo oldValue
    }

    @RepeatedTest(TEST_SIZE)
    fun `putIfAbsent - 값이 없는 경우`() {
        // 등록되지 않는 key2 에 대해서 새로 등록한다 -> backCache에 등록되어 다른 nearCache에 전달되어야 합니다.
        val key = "not-exist-key"
        val value = randomValue()

        nearCache2.putIfAbsent(key, value).shouldBeTrue()
        nearCache2[key] shouldBeEqualTo value

        await until { nearCache1.containsKey(key) }

        backCache[key] shouldBeEqualTo value
        nearCache1[key] shouldBeEqualTo value
    }

    @RepeatedTest(TEST_SIZE)
    fun `remove - cache entry를 삭제하면 모든 near cache에서 삭제됩니다`() {
        val key = randomKey()
        val value = randomValue()

        nearCache1.put(key, value)
        await until { nearCache2.containsKey(key) }

        nearCache1.containsKey(key).shouldBeTrue()
        nearCache2.containsKey(key).shouldBeTrue()
        nearCache2[key] shouldBeEqualTo value

        nearCache2.remove(key)
        await untilNull { nearCache1[key] }

        nearCache1.containsKey(key).shouldBeFalse()
        nearCache2.containsKey(key).shouldBeFalse()
        nearCache1[key].shouldBeNull()
        nearCache2[key].shouldBeNull()
    }

    @RepeatedTest(TEST_SIZE)
    fun `remove with value - cache entry를 삭제하면 모든 near cache에서 삭제됩니다`() {
        val key = randomKey()
        val oldValue = randomValue()
        val newValue = randomValue()

        nearCache1.put(key, newValue)
        await until { nearCache2[key] == newValue }

        nearCache2.put(key, oldValue)
        await until { nearCache1[key] == oldValue }

        // nearCache2에서 update 한 것이 반영되었다
        nearCache1.remove(key, oldValue).shouldBeTrue()
        await untilNull { nearCache2[key] }

        nearCache1.containsKey(key).shouldBeFalse()
        nearCache2.containsKey(key).shouldBeFalse()

        nearCache1.put(key, newValue)
        await until { nearCache2[key] == newValue }

        nearCache2.put(key, oldValue)
        await until { nearCache1[key] == oldValue }

        // 마지막 Layer의 Cache 값이 Update 되어서 oldValue를 가진다.
        nearCache1.remove(key, oldValue).shouldBeTrue()
        await untilNull { nearCache2[key] }

        nearCache1[key].shouldBeNull()
        nearCache2[key].shouldBeNull()

        // 다른 값으로 삭제가 실패할 경우에는 값이 존재한다
        nearCache1.put(key, oldValue)
        nearCache1.remove(key, newValue).shouldBeFalse()
        await until { nearCache2[key] == oldValue }

        nearCache1[key] shouldBeEqualTo oldValue
        nearCache2[key] shouldBeEqualTo oldValue
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and remove - getAndRemove 시 모든 캐시에서 제거된다`() {
        val key = randomKey()
        val value = randomValue()
        val value2 = randomValue()

        nearCache1.put(key, value)
        await until { nearCache2[key] == value }

        nearCache1.getAndRemove(key) shouldBeEqualTo value
        await until { nearCache1[key] == null && nearCache2[key] == null }

        nearCache1.containsKey(key).shouldBeFalse()
        nearCache2.containsKey(key).shouldBeFalse()

        nearCache2.put(key, value)
        await until { nearCache1[key] == value }

        nearCache1.getAndRemove(key) shouldBeEqualTo value
        await until { nearCache2[key] == null }

        nearCache1.containsKey(key).shouldBeFalse()
        nearCache2.containsKey(key).shouldBeFalse()

        nearCache1.put(key, value)
        nearCache2.put(key, value)
        await until { nearCache1[key] == value }

        nearCache1.getAndRemove(key) shouldBeEqualTo value
        await until { nearCache2[key] == null }

        nearCache1.containsKey(key).shouldBeFalse()
        nearCache2.containsKey(key).shouldBeFalse()

        // 마지막 Layer의 변경이 전파된다.
        nearCache1.put(key, value)
        await until { nearCache2[key] == value }
        // BackCache가 변경되면 모든 NearCache에 전파됩니다
        backCache.put(key, value2)
        await until { nearCache1[key] == value2 && nearCache2[key] == value2 }

        nearCache1.get(key) shouldBeEqualTo value2
        nearCache2.get(key) shouldBeEqualTo value2

        nearCache1.getAndRemove(key) shouldBeEqualTo value2
        await until { nearCache2[key] == null }

        nearCache2.containsKey(key).shouldBeFalse()
        backCache.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `replace old value - 모든 캐시에 적용되어야 합니다`() {
        val key = randomKey()
        val oldValue = randomValue()
        val newValue = randomValue()

        nearCache2.put(key, oldValue)
        await until { nearCache1[key] == oldValue }

        nearCache1.replace(key, oldValue, newValue).shouldBeTrue()
        await until { nearCache2[key] == newValue }

        nearCache1.get(key) shouldBeEqualTo newValue
        nearCache2.get(key) shouldBeEqualTo newValue
        backCache.get(key) shouldBeEqualTo newValue

        // 이미 newValue를 가진다
        nearCache2.replace(key, oldValue, newValue).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `repalce - value를 변경하면 모든 캐시에 적용되어야 한다`() {
        val key = randomKey()
        val oldValue = randomValue()
        val newValue = randomValue()

        nearCache1.put(key, oldValue)
        await until { nearCache2.containsKey(key) }

        nearCache2.replace(key, newValue).shouldBeTrue()
        await until { nearCache1[key] == newValue }

        nearCache1.get(key) shouldBeEqualTo newValue

        nearCache1.remove(key)
        await until { nearCache2[key] == null }

        nearCache2.replace(key, newValue).shouldBeFalse()
        nearCache1.replace(key, newValue).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and replace - 새로운 값으로 대체하고, 기존 값을 반환`() {
        val key = randomKey()
        val oldValue = randomKey()
        val oldValue2 = randomValue()
        val newValue = randomValue()

        // 기존에 key가 없으므로 replace 하지 못한다
        nearCache1.getAndReplace(key, oldValue).shouldBeNull()

        nearCache1.put(key, oldValue)
        await until { nearCache2[key] == oldValue }

        // 이제 key가 있으니 oldValue를 반환하고, newValue를 저장한다
        nearCache2.getAndReplace(key, newValue) shouldBeEqualTo oldValue
        await until { nearCache1[key] == newValue }

        nearCache1.get(key) shouldBeEqualTo newValue
        nearCache2.get(key) shouldBeEqualTo newValue

        nearCache1.clear()
        nearCache2.clear()
        await until { nearCache1.count() == 0 && nearCache2.count() == 0 }

        nearCache1.put(key, oldValue)
        await until { nearCache2[key] == oldValue }
        nearCache2.put(key, oldValue2)
        await until { nearCache1[key] == oldValue2 }

        nearCache1.getAndReplace(key, newValue) shouldBeEqualTo oldValue2
        await until { nearCache2[key] == newValue }
        nearCache1.get(key) shouldBeEqualTo newValue
        nearCache2.get(key) shouldBeEqualTo newValue

        // key가 존재하지 않으므로 replace도 하지 않는다
        nearCache1.remove(key)
        await until { nearCache2[key] == null }
        nearCache2.getAndReplace(key, newValue).shouldBeNull()
        nearCache1.containsKey(key).shouldBeFalse()
        nearCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll with keys - 모든 캐시를 삭제하면 다른 캐시에도 반영된다`() {
        val key1 = randomKey()
        val value1 = randomValue()
        val key2 = randomKey()
        val value2 = randomValue()

        nearCache1.put(key1, value1)
        nearCache2.put(key2, value2)
        await until { nearCache1.containsKey(key2) && nearCache2.containsKey(key1) }

        nearCache1.removeAll(key1, key2)
        await until { nearCache2[key1] == null && nearCache2[key2] == null }

        nearCache1.containsKey(key1).shouldBeFalse()
        nearCache1.containsKey(key2).shouldBeFalse()
        nearCache2.containsKey(key1).shouldBeFalse()
        nearCache2.containsKey(key2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    open fun `removeAll - 모든 캐시를 삭제하면 다른 캐시에도 반영된다`() {
        val key1 = randomKey()
        val value1 = randomValue()
        val key2 = randomKey()
        val value2 = randomValue()

        nearCache1.put(key1, value1)
        nearCache2.put(key2, value2)
        await until { nearCache1.containsKey(key2) && nearCache2.containsKey(key1) }

        nearCache1.removeAll()
        await until { nearCache2[key1] == null && nearCache2[key2] == null }

        nearCache1.containsKey(key1).shouldBeFalse()
        nearCache1.containsKey(key2).shouldBeFalse()
        nearCache2.containsKey(key1).shouldBeFalse()
        nearCache2.containsKey(key2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `clear - cache를 clear 합니다 - front cache만 clear 될 뿐 back cache는 유지됩니다`() {
        val key1 = randomKey()
        val value1 = randomValue()
        val key2 = randomKey()
        val value2 = randomValue()

        nearCache1.put(key1, value1)
        await until { nearCache2.containsKey(key1) }

        nearCache2.put(key2, value2)
        await until { nearCache1.containsKey(key2) }

        // 로컬 캐시만 삭제됩니다. backCache는 삭제되지 않습니다.
        nearCache1.clear()

        // frontCache에서 containsKey 를 조회합니다.
        nearCache1.containsKey(key1).shouldBeFalse()
        nearCache1.containsKey(key2).shouldBeFalse()

        // 다른 캐시에는 전파되지 않습니다
        nearCache2.containsKey(key1).shouldBeTrue()
        nearCache2.containsKey(key2).shouldBeTrue()
    }

    @RepeatedTest(TEST_SIZE)
    fun `clearBackCache - back cache를 삭제하지만 전파는 되지 않습니다`() {
        val key1 = randomKey()
        val value1 = randomValue()
        val key2 = randomKey()
        val value2 = randomValue()

        nearCache1.put(key1, value1)
        nearCache2.put(key2, value2)
        await until { nearCache1.containsKey(key2) && nearCache2.containsKey(key1) }

        nearCache1.clearAllCache()

        // front & back cache 모두 삭제한다
        nearCache1.containsKey(key1).shouldBeFalse()
        nearCache1.containsKey(key2).shouldBeFalse()
        backCache.containsKey(key1).shouldBeFalse()
        backCache.containsKey(key2).shouldBeFalse()

        // 다른 nearCache에는 전파되지 않습니다
        nearCache2.containsKey(key1).shouldBeTrue()
        nearCache2.containsKey(key2).shouldBeTrue()
    }

    @Test
    fun `nearCache Close 시, backCache expiration thread 중지된다`(/*output: OutputCapturer*/) {
        val checkExpiryMs = 1000L
        val nearCache = NearCache(NearCacheConfig(checkExpiryPeriod = checkExpiryMs), backCache)

        val key = randomKey()
        val value = randomValue()
        nearCache.put(key, value)

        Thread.sleep(checkExpiryMs * 3)

        nearCache.close()
        await atMost 30.seconds.toJavaDuration() until { nearCache.isClosed }
        Thread.sleep(checkExpiryMs)

        // nearCache가 close 되었으므로
        nearCache.frontCache.isClosed.shouldBeTrue()
    }
}
