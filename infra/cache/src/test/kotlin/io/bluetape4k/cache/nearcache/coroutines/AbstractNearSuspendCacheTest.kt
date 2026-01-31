package io.bluetape4k.cache.nearcache.coroutines

import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCacheEntry
import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.awaitility.suspendUntil
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeEmpty
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.Duration

@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractNearSuspendCacheTest
    : CoroutineScope by CoroutineScope(CoroutineName("near-cocache") + Dispatchers.IO) {

    companion object: KLoggingChannel() {
        private const val TEST_SIZE = 3

        fun getKey() = TimebasedUuid.Reordered.nextIdAsString()
        fun getValue() = Fakers.randomString(1024, 4096, true)
    }

    abstract val backSuspendCache: SuspendCache<String, Any>

    protected val frontCoCache1 = CaffeineSuspendCache<String, Any> {
        this.expireAfterAccess(Duration.ofMinutes(5))
        this.maximumSize(10_000)
    }
    protected val frontCoCache2 = CaffeineSuspendCache<String, Any> {
        this.expireAfterAccess(Duration.ofMinutes(10))
        this.maximumSize(10_000)
    }

    protected val nearSuspendCache1: NearSuspendCache<String, Any> by lazy {
        NearSuspendCache(
            frontCoCache1,
            backSuspendCache,
            1000L
        )
    }
    protected val nearSuspendCache2: NearSuspendCache<String, Any> by lazy {
        NearSuspendCache(
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
            nearSuspendCache1.clear()
            nearSuspendCache2.clear()
            backSuspendCache.clear()
        }
    }

    @RepeatedTest(TEST_SIZE)
    fun `front에 값이 없으면, back cache에 있는 값을 read through 로 가져온다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        nearSuspendCache1.get(key).shouldBeNull()

        backSuspendCache.put(key, value)
        await suspendUntil { nearSuspendCache1.containsKey(key) }

        // get 시에 front 에 없으면 back 에서 가져온다 (CacheEntryEvent 는 비동기이므로 즉시 반영되지는 않습니다)
        nearSuspendCache1.get(key) shouldBeEqualTo value
        nearSuspendCache2.get(key) shouldBeEqualTo value
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 삭제하면 write through로 back cache에서도 삭제되고, 다른 nearCache에서도 삭제된다`() =
        runSuspendIO {
            val key = getKey()
            val value = getValue()

            backSuspendCache.containsKey(key).shouldBeFalse()

            nearSuspendCache1.put(key, value)
            await suspendUntil { nearSuspendCache2.containsKey(key) }

            backSuspendCache.get(key) shouldBeEqualTo value
            nearSuspendCache2.get(key) shouldBeEqualTo value
        }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 삭제하면 back cache도 삭제되고, 다른 nearCache에서도 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        backSuspendCache.containsKey(key).shouldBeFalse()

        nearSuspendCache1.put(key, value)
        await suspendUntil { nearSuspendCache2.containsKey(key) }

        backSuspendCache.get(key) shouldBeEqualTo value
        nearSuspendCache2.get(key) shouldBeEqualTo value

        nearSuspendCache1.remove(key).shouldBeTrue()
        await suspendUntil { !nearSuspendCache2.containsKey(key) }

        backSuspendCache.containsKey(key).shouldBeFalse()
        nearSuspendCache1.containsKey(key).shouldBeFalse()
        nearSuspendCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 update하면, 다른 nearCache에서도 update 된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        backSuspendCache.containsKey(key).shouldBeFalse()

        // nearCoCache1 에 cache entry 를 생성하면, nearCoCache2 에도 비동기적으로 생성된다.
        nearSuspendCache1.put(key, value)
        await suspendUntil { nearSuspendCache2.containsKey(key) }

        backSuspendCache.get(key) shouldBeEqualTo value
        nearSuspendCache2.get(key) shouldBeEqualTo value

        // nearCoCache1 에 cache entry를 update하면, nearCoCache2 에도 비동기적으로 update 된다.
        this@AbstractNearSuspendCacheTest.nearSuspendCache1.replace(key, value, value2).shouldBeTrue()
        await suspendUntil { nearSuspendCache2.get(key) == value2 }

        backSuspendCache.get(key) shouldBeEqualTo value2
        nearSuspendCache2.get(key) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `두 개의 nearCoCache가 서로 변화가 반영된다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        val expected = mapOf(key1 to value1, key2 to value2)

        nearSuspendCache1.put(key1, value1)
        nearSuspendCache2.put(key2, value2)
        await suspendUntil {
            nearSuspendCache1.containsKey(key2) &&
                    nearSuspendCache2.containsKey(key1)
        }

        val actual2 = nearSuspendCache2.getAll(key1, key2).toFastList().associate { it.key to it.value }
        actual2 shouldContainSame expected

        val actual1 = nearSuspendCache1.getAll(key1, key2).toFastList().associate { it.key to it.value }
        actual1 shouldContainSame expected
    }

    @RepeatedTest(TEST_SIZE)
    fun `putAll with map - 복수의 cache entry를 추가하면 다른 nearCache에도 반영된다`() = runSuspendIO {
        val entries = List(10) { getKey() to getValue() }.toMap()
        val keys = entries.keys

        nearSuspendCache1.putAll(entries)
        await suspendUntil { keys.all { nearSuspendCache2.containsKey(it) } }

        nearSuspendCache2.getAll().toFastList() shouldContainSame entries.map { SuspendCacheEntry(it.key, it.value) }
    }

    @RepeatedTest(TEST_SIZE)
    fun `putAll with flow - 복수의 cache entry를 추가하면 다른 nearCache에도 반영된다`() = runSuspendIO {
        val entries = List(10) { getKey() to getValue() }.toMap()
        val keys = entries.keys

        nearSuspendCache1.putAllFlow(entries.map { it.key to it.value }.asFlow())
        await suspendUntil { keys.all { nearSuspendCache2.containsKey(it) } }

        nearSuspendCache2.getAll().toFastList() shouldContainSame entries.map { SuspendCacheEntry(it.key, it.value) }
    }

    @RepeatedTest(TEST_SIZE)
    fun `putIfAbsent - cache entry가 없는 경우에만 추가되고, 전파됩니다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        nearSuspendCache1.put(key, value)
        await suspendUntil { nearSuspendCache2.containsKey(key) }

        // 이미 cache entry 생성이 전파되어 반영되었다.
        nearSuspendCache2.putIfAbsent(key, value2).shouldBeFalse()
        nearSuspendCache2.get(key) shouldBeEqualTo value

        // 존재하지 않는 key2 에 대해서 새로 등록된다.
        val key2 = getKey()
        nearSuspendCache2.putIfAbsent(key2, value2).shouldBeTrue()
        nearSuspendCache2.get(key2) shouldBeEqualTo value2
        await suspendUntil { nearSuspendCache1.containsKey(key2) }

        nearSuspendCache1.get(key2) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `remove with value - cache entry를 삭제하면 모든 nearCache에서 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        nearSuspendCache1.put(key, value)
        await suspendUntil { nearSuspendCache2.containsKey(key) }
        nearSuspendCache2.get(key) shouldBeEqualTo value
        // cache entry가 일치하지 않으면 삭제되지 않는다
        nearSuspendCache2.remove(key, value2).shouldBeFalse()
        // cache entry를 삭제한다
        nearSuspendCache2.remove(key, value).shouldBeTrue()
        await suspendUntil { !nearSuspendCache1.containsKey(key) }

        nearSuspendCache1.containsKey(key).shouldBeFalse()
        nearSuspendCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and remove - 모든 nearCache에서 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        nearSuspendCache1.put(key, value)
        await suspendUntil { nearSuspendCache2.containsKey(key) }

        nearSuspendCache1.containsKey(key).shouldBeTrue()
        nearSuspendCache2.containsKey(key).shouldBeTrue()

        nearSuspendCache1.getAndRemove(key) shouldBeEqualTo value
        await suspendUntil { !nearSuspendCache2.containsKey(key) }

        nearSuspendCache1.containsKey(key).shouldBeFalse()
        nearSuspendCache2.containsKey(key).shouldBeFalse()

        backSuspendCache.put(key, value2)
        await suspendUntil { nearSuspendCache1.containsKey(key) }

        nearSuspendCache1.getAndRemove(key) shouldBeEqualTo value2
        await suspendUntil { !nearSuspendCache2.containsKey(key) }

        nearSuspendCache1.containsKey(key).shouldBeFalse()
        nearSuspendCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `replace old value - 모든 nearCache에서 update된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        nearSuspendCache2.put(key, value)
        await suspendUntil { nearSuspendCache1.containsKey(key) }

        this@AbstractNearSuspendCacheTest.nearSuspendCache1.replace(key, value, value2).shouldBeTrue()
        await suspendUntil { nearSuspendCache2.get(key) == value2 }

        nearSuspendCache2.get(key) shouldBeEqualTo value2

        // 이미 key-value2 로 갱신되었으므로 update에 실패한다 
        nearSuspendCache2.replace(key, value, value2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `replace - 모든 nearCache가 update 된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        // 존재하지 않는 key 이므로 replace하지 못한다  
        nearSuspendCache1.replace(key, value).shouldBeFalse()

        nearSuspendCache1.put(key, value)
        await suspendUntil { nearSuspendCache2.containsKey(key) }

        nearSuspendCache2.replace(key, value2).shouldBeTrue()
        await suspendUntil { nearSuspendCache1.get(key) == value2 }

        nearSuspendCache1.get(key) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and replace - 기존 값을 가져오고 새로운 값으로 갱신한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()
        val value3 = getValue()

        // key가 없으므로 replace 하지 못한다
        nearSuspendCache1.getAndReplace(key, value).shouldBeNull()
        nearSuspendCache1.containsKey(key).shouldBeFalse()

        nearSuspendCache1.put(key, value)
        await suspendUntil { nearSuspendCache2.containsKey(key) }

        // key 가 등록되어 있으므로, replace를 수행한다
        nearSuspendCache2.getAndReplace(key, value2) shouldBeEqualTo value
        nearSuspendCache2.get(key) shouldBeEqualTo value2
        await suspendUntil { nearSuspendCache1.get(key) == value2 }
        nearSuspendCache1.get(key) shouldBeEqualTo value2

        nearSuspendCache1.put(key, value)
        await suspendUntil { nearSuspendCache2.get(key) == value }

        nearSuspendCache2.put(key, value2)
        await suspendUntil { nearSuspendCache1.get(key) == value2 }

        nearSuspendCache1.getAndReplace(key, value3) shouldBeEqualTo value2
        nearSuspendCache1.get(key) shouldBeEqualTo value3
        await suspendUntil { nearSuspendCache2.get(key) == value3 }

        nearSuspendCache2.get(key) shouldBeEqualTo value3
    }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll with keys - 지정한 key 들을 삭제하면 모든 nearCache에 반영된다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        nearSuspendCache1.put(key1, value1)
        nearSuspendCache2.put(key2, value2)
        await suspendUntil {
            nearSuspendCache1.containsKey(key2) &&
                    nearSuspendCache2.containsKey(key1)
        }

        nearSuspendCache2.removeAll(key1, key2)
        await suspendUntil {
            !nearSuspendCache1.containsKey(key1) &&
                    !nearSuspendCache1.containsKey(key2)
        }

        nearSuspendCache1.containsKey(key1).shouldBeFalse()
        nearSuspendCache1.containsKey(key2).shouldBeFalse()
        nearSuspendCache2.containsKey(key1).shouldBeFalse()
        nearSuspendCache2.containsKey(key2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll - 모든 캐시를 삭제하면 nearCache들에게 반영된다`() = runSuspendIO {
        val entries = List(100) { getKey() to getValue() }.toMap()

        nearSuspendCache1.putAll(entries)
        await suspendUntil { nearSuspendCache2.entries().count() > 0 }

        nearSuspendCache2.entries().toFastList().shouldNotBeEmpty()

        // 모든 cache entry를 삭제하면 backCache에서 삭제되고, 이것이 전파되어 nearCache1에서도 삭제된다.
        nearSuspendCache2.removeAll()
        await suspendUntil { nearSuspendCache1.entries().count() == 0 }

        nearSuspendCache1.entries().count() shouldBeEqualTo 0
    }

    @RepeatedTest(TEST_SIZE)
    fun `clear - front cache만 clear 합니다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        nearSuspendCache1.put(key1, value1)
        nearSuspendCache2.put(key2, value2)
        await suspendUntil {
            nearSuspendCache1.containsKey(key2) &&
                    nearSuspendCache2.containsKey(key1)
        }

        nearSuspendCache1.clear()

        // front cache에만 삭제되었고, bach cache는 유지된다 
        nearSuspendCache1.containsKey(key1).shouldBeTrue()
        nearSuspendCache1.containsKey(key2).shouldBeTrue()

        // 다른 near cache에는 반영안된다.
        nearSuspendCache2.containsKey(key1).shouldBeTrue()
        nearSuspendCache2.containsKey(key2).shouldBeTrue()
    }

    @RepeatedTest(TEST_SIZE)
    fun `clearAll - front cache와 back cache 모두를 clear 합니다 - 전파는 되지 않습니다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        nearSuspendCache1.put(key1, value1)
        nearSuspendCache2.put(key2, value2)

        await suspendUntil {
            nearSuspendCache1.containsKey(key2) &&
                    nearSuspendCache2.containsKey(key1)
        }

        // nearCache1 과 backCache 는 clear 되지만, nearCache2 로는 전파되지 않는다
        nearSuspendCache1.clearAll()

        // front cache, back cache 모두를 clear 합니다.
        nearSuspendCache1.containsKey(key1).shouldBeFalse()
        nearSuspendCache1.containsKey(key2).shouldBeFalse()

        // 다른 near cache에는 반영안된다. - removeAll() 을 사용해야 다른 nearCache에도 반영됩니다.
        nearSuspendCache2.containsKey(key1).shouldBeTrue()
        nearSuspendCache2.containsKey(key2).shouldBeTrue()
    }
}
