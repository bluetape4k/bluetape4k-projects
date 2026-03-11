package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.cache.jcache.SuspendCacheEntry
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
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

@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractSuspendNearCacheTest
    : CoroutineScope by CoroutineScope(CoroutineName("near-cocache") + Dispatchers.IO) {

    companion object: KLoggingChannel() {
        private const val TEST_SIZE = 3

        fun getKey() = Base58.randomString(16)
        fun getValue() = Fakers.randomString(1024, 4096, true)
    }

    abstract val backSuspendCache: SuspendCache<String, Any>
    protected abstract fun createFrontSuspendCache(expireAfterAccess: Duration): SuspendCache<String, Any>

    protected val frontCoCache1 by lazy { createFrontSuspendCache(Duration.ofMinutes(5)) }
    protected val frontCoCache2 by lazy { createFrontSuspendCache(Duration.ofMinutes(10)) }

    protected val suspendNearCache1: SuspendNearCache<String, Any> by lazy {
        SuspendNearCache(
            frontCoCache1,
            backSuspendCache,
            1000L
        )
    }
    protected val suspendNearCache2: SuspendNearCache<String, Any> by lazy {
        SuspendNearCache(
            frontCoCache2,
            backSuspendCache,
            1000L
        )
    }

    @BeforeEach
    fun setup() {
        // clear 는 front cache 에만 적용.
        // clearAll 은 front, back cache 모두에 적용
        runSuspendIO {
            suspendNearCache1.clear()
            suspendNearCache2.clear()
            backSuspendCache.clear()
        }
    }

    @RepeatedTest(TEST_SIZE)
    fun `front에 값이 없으면, back cache에 있는 값을 read through 로 가져온다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        suspendNearCache1.get(key).shouldBeNull()

        backSuspendCache.put(key, value)
        await untilSuspending { suspendNearCache1.containsKey(key) }

        // get 시에 front 에 없으면 back 에서 가져온다 (CacheEntryEvent 는 비동기이므로 즉시 반영되지는 않습니다)
        suspendNearCache1.get(key) shouldBeEqualTo value
        suspendNearCache2.get(key) shouldBeEqualTo value
    }

    // TODO: 실제 시나리오를 만들기 힘듬 (시점 차이) -> Mockk 로 대체해야 함
    @Disabled("시나리오 미비 -> Mockk 으로 대체해야 함")
    @RepeatedTest(TEST_SIZE)
    fun `getDeeply - front miss면 back cache에서 조회하고 front cache를 채운다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        backSuspendCache.put(key, value)
        suspendNearCache1.clear()

        suspendNearCache1.getDeeply(key) shouldBeEqualTo value
        await untilSuspending { suspendNearCache1.containsKey(key) }
        suspendNearCache1.get(key) shouldBeEqualTo value
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 삭제하면 write through로 back cache에서도 삭제되고, 다른 nearCache에서도 삭제된다`() =
        runSuspendIO {
            val key = getKey()
            val value = getValue()

            backSuspendCache.containsKey(key).shouldBeFalse()

            suspendNearCache1.put(key, value)
            await untilSuspending { suspendNearCache2.containsKey(key) }

            backSuspendCache.get(key) shouldBeEqualTo value
            suspendNearCache2.get(key) shouldBeEqualTo value
        }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 삭제하면 back cache도 삭제되고, 다른 nearCache에서도 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        backSuspendCache.containsKey(key).shouldBeFalse()

        suspendNearCache1.put(key, value)
        await untilSuspending { suspendNearCache2.containsKey(key) }

        backSuspendCache.get(key) shouldBeEqualTo value
        suspendNearCache2.get(key) shouldBeEqualTo value

        suspendNearCache1.remove(key).shouldBeTrue()
        await untilSuspending { !suspendNearCache2.containsKey(key) }

        backSuspendCache.containsKey(key).shouldBeFalse()
        suspendNearCache1.containsKey(key).shouldBeFalse()
        suspendNearCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 update하면, 다른 nearCache에서도 update 된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        backSuspendCache.containsKey(key).shouldBeFalse()

        // nearCoCache1 에 cache entry 를 생성하면, nearCoCache2 에도 비동기적으로 생성된다.
        suspendNearCache1.put(key, value)
        await untilSuspending { suspendNearCache2.containsKey(key) }

        backSuspendCache.get(key) shouldBeEqualTo value
        suspendNearCache2.get(key) shouldBeEqualTo value

        // nearCoCache1 에 cache entry를 update하면, nearCoCache2 에도 비동기적으로 update 된다.
        suspendNearCache1.replace(key, value, value2).shouldBeTrue()
        await untilSuspending { suspendNearCache2.get(key) == value2 }

        backSuspendCache.get(key) shouldBeEqualTo value2
        suspendNearCache2.get(key) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `두 개의 nearCoCache가 서로 변화가 반영된다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        val expected = mapOf(key1 to value1, key2 to value2)

        suspendNearCache1.put(key1, value1)
        suspendNearCache2.put(key2, value2)
        await untilSuspending {
            suspendNearCache1.containsKey(key2) &&
                    suspendNearCache2.containsKey(key1)
        }

        val actual2 = suspendNearCache2.getAll(key1, key2).toList().associate { it.key to it.value }
        actual2 shouldContainSame expected

        val actual1 = suspendNearCache1.getAll(key1, key2).toList().associate { it.key to it.value }
        actual1 shouldContainSame expected
    }

    @RepeatedTest(TEST_SIZE)
    fun `putAll with map - 복수의 cache entry를 추가하면 다른 nearCache에도 반영된다`() = runSuspendIO {
        val map = List(10) { getKey() to getValue() }.toMap()
        val keys = map.keys

        suspendNearCache1.putAll(map)
        await untilSuspending { keys.all { suspendNearCache2.containsKey(it) } }

        suspendNearCache2.getAll().toList() shouldContainSame map.entries.map {
            SuspendCacheEntry(
                it.key,
                it.value
            )
        }
    }

    @RepeatedTest(TEST_SIZE)
    fun `putAll with flow - 복수의 cache entry를 추가하면 다른 nearCache에도 반영된다`() = runSuspendIO {
        val entries = List(10) { getKey() to getValue() }.toMap()
        val keys = entries.keys

        suspendNearCache1.putAllFlow(entries.map { it.key to it.value }.asFlow())
        await untilSuspending { keys.all { suspendNearCache2.containsKey(it) } }

        suspendNearCache2.getAll().toList() shouldContainSame entries.map { SuspendCacheEntry(it.key, it.value) }
    }

    @RepeatedTest(TEST_SIZE)
    fun `putIfAbsent - cache entry가 없는 경우에만 추가되고, 전파됩니다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendNearCache1.put(key, value)
        await untilSuspending { suspendNearCache2.containsKey(key) }

        // 이미 cache entry 생성이 전파되어 반영되었다.
        suspendNearCache2.putIfAbsent(key, value2).shouldBeFalse()
        suspendNearCache2.get(key) shouldBeEqualTo value

        // 존재하지 않는 key2 에 대해서 새로 등록된다.
        val key2 = getKey()
        suspendNearCache2.putIfAbsent(key2, value2).shouldBeTrue()
        suspendNearCache2.get(key2) shouldBeEqualTo value2
        await untilSuspending { suspendNearCache1.containsKey(key2) }

        suspendNearCache1.get(key2) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `remove with value - cache entry를 삭제하면 모든 nearCache에서 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendNearCache1.put(key, value)
        await untilSuspending { suspendNearCache2.containsKey(key) }
        suspendNearCache2.get(key) shouldBeEqualTo value
        // cache entry가 일치하지 않으면 삭제되지 않는다
        suspendNearCache2.remove(key, value2).shouldBeFalse()
        // cache entry를 삭제한다
        suspendNearCache2.remove(key, value).shouldBeTrue()
        await untilSuspending { !suspendNearCache1.containsKey(key) }

        suspendNearCache1.containsKey(key).shouldBeFalse()
        suspendNearCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and remove - 모든 nearCache에서 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendNearCache1.put(key, value)
        await untilSuspending { suspendNearCache2.containsKey(key) }

        suspendNearCache1.containsKey(key).shouldBeTrue()
        suspendNearCache2.containsKey(key).shouldBeTrue()

        suspendNearCache1.getAndRemove(key) shouldBeEqualTo value
        await untilSuspending { !suspendNearCache2.containsKey(key) }

        suspendNearCache1.containsKey(key).shouldBeFalse()
        suspendNearCache2.containsKey(key).shouldBeFalse()

        backSuspendCache.put(key, value2)
        await untilSuspending { suspendNearCache1.containsKey(key) }

        suspendNearCache1.getAndRemove(key) shouldBeEqualTo value2
        await untilSuspending { !suspendNearCache2.containsKey(key) }

        suspendNearCache1.containsKey(key).shouldBeFalse()
        suspendNearCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `replace old value - 모든 nearCache에서 update된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendNearCache2.put(key, value)
        await untilSuspending { suspendNearCache1.containsKey(key) }

        this@AbstractSuspendNearCacheTest.suspendNearCache1.replace(key, value, value2).shouldBeTrue()
        await untilSuspending { suspendNearCache2.get(key) == value2 }

        suspendNearCache2.get(key) shouldBeEqualTo value2

        // 이미 key-value2 로 갱신되었으므로 update에 실패한다 
        suspendNearCache2.replace(key, value, value2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `replace - 모든 nearCache가 update 된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        // 존재하지 않는 key 이므로 replace하지 못한다  
        suspendNearCache1.replace(key, value).shouldBeFalse()

        suspendNearCache1.put(key, value)
        await untilSuspending { suspendNearCache2.containsKey(key) }

        suspendNearCache2.replace(key, value2).shouldBeTrue()
        await untilSuspending { suspendNearCache1.get(key) == value2 }

        suspendNearCache1.get(key) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and replace - 기존 값을 가져오고 새로운 값으로 갱신한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()
        val value3 = getValue()

        // key가 없으므로 replace 하지 못한다
        suspendNearCache1.getAndReplace(key, value).shouldBeNull()
        suspendNearCache1.containsKey(key).shouldBeFalse()

        suspendNearCache1.put(key, value)
        await untilSuspending { suspendNearCache2.containsKey(key) }

        // key 가 등록되어 있으므로, replace를 수행한다
        suspendNearCache2.getAndReplace(key, value2) shouldBeEqualTo value
        suspendNearCache2.get(key) shouldBeEqualTo value2
        await untilSuspending { suspendNearCache1.get(key) == value2 }
        suspendNearCache1.get(key) shouldBeEqualTo value2

        suspendNearCache1.put(key, value)
        await untilSuspending { suspendNearCache2.get(key) == value }

        suspendNearCache2.put(key, value2)
        await untilSuspending { suspendNearCache1.get(key) == value2 }

        suspendNearCache1.getAndReplace(key, value3) shouldBeEqualTo value2
        suspendNearCache1.get(key) shouldBeEqualTo value3
        await untilSuspending { suspendNearCache2.get(key) == value3 }

        suspendNearCache2.get(key) shouldBeEqualTo value3
    }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll with keys - 지정한 key 들을 삭제하면 모든 nearCache에 반영된다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        suspendNearCache1.put(key1, value1)
        suspendNearCache2.put(key2, value2)
        await untilSuspending {
            suspendNearCache1.containsKey(key2) &&
                    suspendNearCache2.containsKey(key1)
        }

        suspendNearCache2.removeAll(key1, key2)
        await untilSuspending {
            !suspendNearCache1.containsKey(key1) &&
                    !suspendNearCache1.containsKey(key2)
        }

        suspendNearCache1.containsKey(key1).shouldBeFalse()
        suspendNearCache1.containsKey(key2).shouldBeFalse()
        suspendNearCache2.containsKey(key1).shouldBeFalse()
        suspendNearCache2.containsKey(key2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll - 모든 캐시를 삭제하면 nearCache들에게 반영된다`() = runSuspendIO {
        val map = List(100) { getKey() to getValue() }.toMap()

        suspendNearCache1.putAll(map)
        await untilSuspending { suspendNearCache2.entries().count() > 0 }

        suspendNearCache2.entries().toList().shouldNotBeEmpty()

        // 모든 cache entry를 삭제하면 backCache에서 삭제되고, 이것이 전파되어 nearCache1에서도 삭제된다.
        suspendNearCache2.removeAll()
        await untilSuspending { suspendNearCache1.entries().count() == 0 }

        suspendNearCache1.entries().count() shouldBeEqualTo 0
    }

    @RepeatedTest(TEST_SIZE)
    fun `clear - front cache만 clear 합니다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        suspendNearCache1.put(key1, value1)
        suspendNearCache2.put(key2, value2)
        await untilSuspending {
            suspendNearCache1.containsKey(key2) &&
                    suspendNearCache2.containsKey(key1)
        }

        suspendNearCache1.clear()

        // front cache에만 삭제되었고, bach cache는 유지된다 
        suspendNearCache1.containsKey(key1).shouldBeTrue()
        suspendNearCache1.containsKey(key2).shouldBeTrue()

        // 다른 near cache에는 반영안된다.
        suspendNearCache2.containsKey(key1).shouldBeTrue()
        suspendNearCache2.containsKey(key2).shouldBeTrue()
    }

    @RepeatedTest(TEST_SIZE)
    fun `clearAll - front cache와 back cache 모두를 clear 합니다 - 전파는 되지 않습니다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        suspendNearCache1.put(key1, value1)
        suspendNearCache2.put(key2, value2)

        await untilSuspending {
            suspendNearCache1.containsKey(key2) &&
                    suspendNearCache2.containsKey(key1)
        }

        // nearCache1 과 backCache 는 clear 되지만, nearCache2 로는 전파되지 않는다
        suspendNearCache1.clearAll()

        // front cache, back cache 모두를 clear 합니다.
        suspendNearCache1.containsKey(key1).shouldBeFalse()
        suspendNearCache1.containsKey(key2).shouldBeFalse()

        // 다른 near cache에는 반영안된다. - removeAll() 을 사용해야 다른 nearCache에도 반영됩니다.
        suspendNearCache2.containsKey(key1).shouldBeTrue()
        suspendNearCache2.containsKey(key2).shouldBeTrue()
    }
}
