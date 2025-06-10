package io.bluetape4k.cache.nearcache.coroutines

import io.bluetape4k.cache.jcache.coroutines.CaffeineCoCache
import io.bluetape4k.cache.jcache.coroutines.CoCache
import io.bluetape4k.cache.jcache.coroutines.CoCacheEntry
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.awaitility.coUntil
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeEmpty
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.Duration

@Disabled("NearCoCacheTest is deprecated, use NearSuspendCacheTest instead")
@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractNearCoCacheTest
    : CoroutineScope by CoroutineScope(CoroutineName("near-cocache") + Dispatchers.IO) {

    companion object: KLoggingChannel() {
        private const val TEST_SIZE = 3

        fun getKey() = TimebasedUuid.nextBase62String()
        fun getValue() = Fakers.randomString(1024, 4096, true)
    }

    abstract val backCoCache: CoCache<String, Any>

    protected val frontCoCache1 = CaffeineCoCache<String, Any> {
        this.expireAfterAccess(Duration.ofMinutes(5))
        this.maximumSize(10_000)
    }
    protected val frontCoCache2 = CaffeineCoCache<String, Any> {
        this.expireAfterAccess(Duration.ofMinutes(10))
        this.maximumSize(10_000)
    }

    protected val nearCoCache1: NearCoCache<String, Any> by lazy { NearCoCache(frontCoCache1, backCoCache, 1000L) }
    protected val nearCoCache2: NearCoCache<String, Any> by lazy { NearCoCache(frontCoCache2, backCoCache, 1000L) }

    @BeforeEach
    fun setup() {
        // clear 는 front cache 에만 적용.
        // clearAll 은 front, back cache 모두에 적용
        runSuspendIO {
            nearCoCache1.clear()
            nearCoCache2.clear()
            backCoCache.clear()
        }
    }

    @RepeatedTest(TEST_SIZE)
    fun `front에 값이 없으면, back cache에 있는 값을 read through 로 가져온다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        nearCoCache1.get(key).shouldBeNull()

        backCoCache.put(key, value)
        await coUntil { nearCoCache1.containsKey(key) }

        // get 시에 front 에 없으면 back 에서 가져온다 (CacheEntryEvent 는 비동기이므로 즉시 반영되지는 않습니다)
        nearCoCache1.get(key) shouldBeEqualTo value
        nearCoCache2.get(key) shouldBeEqualTo value
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 삭제하면 write through로 back cache에서도 삭제되고, 다른 nearCache에서도 삭제된다`() =
        runSuspendIO {
            val key = getKey()
            val value = getValue()

            backCoCache.containsKey(key).shouldBeFalse()

            nearCoCache1.put(key, value)
            await coUntil { nearCoCache2.containsKey(key) }

            backCoCache.get(key) shouldBeEqualTo value
            nearCoCache2.get(key) shouldBeEqualTo value
        }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 삭제하면 back cache도 삭제되고, 다른 nearCache에서도 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        backCoCache.containsKey(key).shouldBeFalse()

        nearCoCache1.put(key, value)
        await coUntil { nearCoCache2.containsKey(key) }

        backCoCache.get(key) shouldBeEqualTo value
        nearCoCache2.get(key) shouldBeEqualTo value

        nearCoCache1.remove(key).shouldBeTrue()
        await coUntil { !nearCoCache2.containsKey(key) }

        backCoCache.containsKey(key).shouldBeFalse()
        nearCoCache1.containsKey(key).shouldBeFalse()
        nearCoCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 update하면, 다른 nearCache에서도 update 된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        backCoCache.containsKey(key).shouldBeFalse()

        // nearCoCache1 에 cache entry 를 생성하면, nearCoCache2 에도 비동기적으로 생성된다.
        nearCoCache1.put(key, value)
        await coUntil { nearCoCache2.containsKey(key) }

        backCoCache.get(key) shouldBeEqualTo value
        nearCoCache2.get(key) shouldBeEqualTo value

        // nearCoCache1 에 cache entry를 update하면, nearCoCache2 에도 비동기적으로 update 된다.
        nearCoCache1.replace(key, value, value2).shouldBeTrue()
        await coUntil { nearCoCache2.get(key) == value2 }

        backCoCache.get(key) shouldBeEqualTo value2
        nearCoCache2.get(key) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `두 개의 nearCoCache가 서로 변화가 반영된다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        val expected = mapOf(key1 to value1, key2 to value2)

        nearCoCache1.put(key1, value1)
        nearCoCache2.put(key2, value2)
        await coUntil {
            nearCoCache1.containsKey(key2) &&
                    nearCoCache2.containsKey(key1)
        }

        val actual2 = nearCoCache2.getAll(key1, key2).map { it.key to it.value }.toList().toMap()
        actual2 shouldContainSame expected

        val actual1 = nearCoCache1.getAll(key1, key2).map { it.key to it.value }.toList().toMap()
        actual1 shouldContainSame expected
    }

    @RepeatedTest(TEST_SIZE)
    fun `putAll with map - 복수의 cache entry를 추가하면 다른 nearCache에도 반영된다`() = runSuspendIO {
        val entries = List(10) { getKey() to getValue() }.toMap()
        val keys = entries.keys

        nearCoCache1.putAll(entries)
        await coUntil { keys.all { nearCoCache2.containsKey(it) } }

        nearCoCache2.getAll().toList() shouldContainSame entries.map { CoCacheEntry(it.key, it.value) }
    }

    @RepeatedTest(TEST_SIZE)
    fun `putAll with flow - 복수의 cache entry를 추가하면 다른 nearCache에도 반영된다`() = runSuspendIO {
        val entries = List(10) { getKey() to getValue() }.toMap()
        val keys = entries.keys

        nearCoCache1.putAllFlow(entries.map { it.key to it.value }.asFlow())
        await coUntil { keys.all { nearCoCache2.containsKey(it) } }

        nearCoCache2.getAll().toList() shouldContainSame entries.map { CoCacheEntry(it.key, it.value) }
    }

    @RepeatedTest(TEST_SIZE)
    fun `putIfAbsent - cache entry가 없는 경우에만 추가되고, 전파됩니다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        nearCoCache1.put(key, value)
        await coUntil { nearCoCache2.containsKey(key) }

        // 이미 cache entry 생성이 전파되어 반영되었다.
        nearCoCache2.putIfAbsent(key, value2).shouldBeFalse()
        nearCoCache2.get(key) shouldBeEqualTo value

        // 존재하지 않는 key2 에 대해서 새로 등록된다.
        val key2 = getKey()
        nearCoCache2.putIfAbsent(key2, value2).shouldBeTrue()
        nearCoCache2.get(key2) shouldBeEqualTo value2
        await coUntil { nearCoCache1.containsKey(key2) }

        nearCoCache1.get(key2) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `remove with value - cache entry를 삭제하면 모든 nearCache에서 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        nearCoCache1.put(key, value)
        await coUntil { nearCoCache2.containsKey(key) }
        nearCoCache2.get(key) shouldBeEqualTo value
        // cache entry가 일치하지 않으면 삭제되지 않는다
        nearCoCache2.remove(key, value2).shouldBeFalse()
        // cache entry를 삭제한다
        nearCoCache2.remove(key, value).shouldBeTrue()
        await coUntil { !nearCoCache1.containsKey(key) }

        nearCoCache1.containsKey(key).shouldBeFalse()
        nearCoCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and remove - 모든 nearCache에서 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        nearCoCache1.put(key, value)
        await coUntil { nearCoCache2.containsKey(key) }

        nearCoCache1.containsKey(key).shouldBeTrue()
        nearCoCache2.containsKey(key).shouldBeTrue()

        nearCoCache1.getAndRemove(key) shouldBeEqualTo value
        await coUntil { !nearCoCache2.containsKey(key) }

        nearCoCache1.containsKey(key).shouldBeFalse()
        nearCoCache2.containsKey(key).shouldBeFalse()

        backCoCache.put(key, value2)
        await coUntil { nearCoCache1.containsKey(key) }

        nearCoCache1.getAndRemove(key) shouldBeEqualTo value2
        await coUntil { !nearCoCache2.containsKey(key) }

        nearCoCache1.containsKey(key).shouldBeFalse()
        nearCoCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `replace old value - 모든 nearCache에서 update된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        nearCoCache2.put(key, value)
        await coUntil { nearCoCache1.containsKey(key) }

        nearCoCache1.replace(key, value, value2).shouldBeTrue()
        await coUntil { nearCoCache2.get(key) == value2 }

        nearCoCache2.get(key) shouldBeEqualTo value2

        // 이미 key-value2 로 갱신되었으므로 update에 실패한다 
        nearCoCache2.replace(key, value, value2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `replace - 모든 nearCache가 update 된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        // 존재하지 않는 key 이므로 replace하지 못한다  
        nearCoCache1.replace(key, value).shouldBeFalse()

        nearCoCache1.put(key, value)
        await coUntil { nearCoCache2.containsKey(key) }

        nearCoCache2.replace(key, value2).shouldBeTrue()
        await coUntil { nearCoCache1.get(key) == value2 }

        nearCoCache1.get(key) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and replace - 기존 값을 가져오고 새로운 값으로 갱신한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()
        val value3 = getValue()

        // key가 없으므로 replace 하지 못한다
        nearCoCache1.getAndReplace(key, value).shouldBeNull()
        nearCoCache1.containsKey(key).shouldBeFalse()

        nearCoCache1.put(key, value)
        await coUntil { nearCoCache2.containsKey(key) }

        // key 가 등록되어 있으므로, replace를 수행한다
        nearCoCache2.getAndReplace(key, value2) shouldBeEqualTo value
        nearCoCache2.get(key) shouldBeEqualTo value2
        await coUntil { nearCoCache1.get(key) == value2 }
        nearCoCache1.get(key) shouldBeEqualTo value2

        nearCoCache1.put(key, value)
        await coUntil { nearCoCache2.get(key) == value }

        nearCoCache2.put(key, value2)
        await coUntil { nearCoCache1.get(key) == value2 }

        nearCoCache1.getAndReplace(key, value3) shouldBeEqualTo value2
        nearCoCache1.get(key) shouldBeEqualTo value3
        await coUntil { nearCoCache2.get(key) == value3 }

        nearCoCache2.get(key) shouldBeEqualTo value3
    }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll with keys - 지정한 key 들을 삭제하면 모든 nearCache에 반영된다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        nearCoCache1.put(key1, value1)
        nearCoCache2.put(key2, value2)
        await coUntil {
            nearCoCache1.containsKey(key2) &&
                    nearCoCache2.containsKey(key1)
        }

        nearCoCache2.removeAll(key1, key2)
        await coUntil {
            !nearCoCache1.containsKey(key1) &&
                    !nearCoCache1.containsKey(key2)
        }

        nearCoCache1.containsKey(key1).shouldBeFalse()
        nearCoCache1.containsKey(key2).shouldBeFalse()
        nearCoCache2.containsKey(key1).shouldBeFalse()
        nearCoCache2.containsKey(key2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll - 모든 캐시를 삭제하면 nearCache들에게 반영된다`() = runSuspendIO {
        val entries = List(100) { getKey() to getValue() }.toMap()

        nearCoCache1.putAll(entries)
        await coUntil { nearCoCache2.entries().toList().isNotEmpty() }

        nearCoCache2.entries().toList().shouldNotBeEmpty()

        // 모든 cache entry를 삭제하면 backCache에서 삭제되고, 이것이 전파되어 nearCache1에서도 삭제된다.
        nearCoCache2.removeAll()
        await coUntil { nearCoCache1.entries().toList().isEmpty() }

        nearCoCache1.entries().toList().shouldBeEmpty()
    }

    @RepeatedTest(TEST_SIZE)
    fun `clear - front cache만 clear 합니다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        nearCoCache1.put(key1, value1)
        nearCoCache2.put(key2, value2)
        await coUntil {
            nearCoCache1.containsKey(key2) &&
                    nearCoCache2.containsKey(key1)
        }

        nearCoCache1.clear()

        // front cache에만 삭제되었고, bach cache는 유지된다 
        nearCoCache1.containsKey(key1).shouldBeTrue()
        nearCoCache1.containsKey(key2).shouldBeTrue()

        // 다른 near cache에는 반영안된다.
        nearCoCache2.containsKey(key1).shouldBeTrue()
        nearCoCache2.containsKey(key2).shouldBeTrue()
    }

    @RepeatedTest(TEST_SIZE)
    fun `clearAll - front cache와 back cache 모두를 clear 합니다 - 전파는 되지 않습니다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        nearCoCache1.put(key1, value1)
        nearCoCache2.put(key2, value2)

        await coUntil {
            nearCoCache1.containsKey(key2) &&
                    nearCoCache2.containsKey(key1)
        }

        // nearCache1 과 backCache 는 clear 되지만, nearCache2 로는 전파되지 않는다
        nearCoCache1.clearAll()

        // front cache, back cache 모두를 clear 합니다.
        nearCoCache1.containsKey(key1).shouldBeFalse()
        nearCoCache1.containsKey(key2).shouldBeFalse()

        // 다른 near cache에는 반영안된다. - removeAll() 을 사용해야 다른 nearCache에도 반영됩니다.
        nearCoCache2.containsKey(key1).shouldBeTrue()
        nearCoCache2.containsKey(key2).shouldBeTrue()
    }
}
