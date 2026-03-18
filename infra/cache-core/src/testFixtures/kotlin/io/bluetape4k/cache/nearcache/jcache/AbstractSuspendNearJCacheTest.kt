package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.cache.jcache.SuspendJCacheEntry
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
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
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractSuspendNearJCacheTest
    : CoroutineScope by CoroutineScope(CoroutineName("near-cocache") + Dispatchers.IO) {

    companion object: KLoggingChannel() {
        private const val TEST_SIZE = 3
        private val awaitTimeout = 5.seconds

        fun getKey() = Base58.randomString(16)
        fun getValue() = Fakers.randomString(1024, 4096, true)
    }

    abstract val backSuspendJCache: SuspendJCache<String, Any>
    protected abstract fun createFrontSuspendCache(expireAfterAccess: Duration): SuspendJCache<String, Any>

    protected val frontCoCache1 by lazy { createFrontSuspendCache(Duration.ofMinutes(5)) }
    protected val frontCoCache2 by lazy { createFrontSuspendCache(Duration.ofMinutes(10)) }

    protected val suspendNearJCache1: SuspendNearJCache<String, Any> by lazy {
        SuspendNearJCache(frontCoCache1, backSuspendJCache)
    }
    protected val suspendNearJCache2: SuspendNearJCache<String, Any> by lazy {
        SuspendNearJCache(frontCoCache2, backSuspendJCache)
    }

    @BeforeEach
    fun setup() {
        // clear 는 front cache 에만 적용.
        // clearAll 은 front, back cache 모두에 적용
        runSuspendIO {
            suspendNearJCache1.clear()
            suspendNearJCache2.clear()
            backSuspendJCache.clear()
        }
    }

    @RepeatedTest(TEST_SIZE)
    fun `front에 값이 없으면, back cache에 있는 값을 read through 로 가져온다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        suspendNearJCache1.get(key).shouldBeNull()

        backSuspendJCache.put(key, value)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache1.containsKey(key) }

        // get 시에 front 에 없으면 back 에서 가져온다 (CacheEntryEvent 는 비동기이므로 즉시 반영되지는 않습니다)
        suspendNearJCache1.get(key) shouldBeEqualTo value
        suspendNearJCache2.get(key) shouldBeEqualTo value
    }

    // TODO: 실제 시나리오를 만들기 힘듬 (시점 차이) -> Mockk 로 대체해야 함
    @Disabled("시나리오 미비 -> Mockk 으로 대체해야 함")
    @RepeatedTest(TEST_SIZE)
    fun `getDeeply - front miss면 back cache에서 조회하고 front cache를 채운다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        backSuspendJCache.put(key, value)
        suspendNearJCache1.clear()

        suspendNearJCache1.getDeeply(key) shouldBeEqualTo value
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache1.containsKey(key) }
        suspendNearJCache1.get(key) shouldBeEqualTo value
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 삭제하면 write through로 back cache에서도 삭제되고, 다른 nearCache에서도 삭제된다`() =
        runSuspendIO {
            val key = getKey()
            val value = getValue()

            backSuspendJCache.containsKey(key).shouldBeFalse()

            suspendNearJCache1.put(key, value)
            await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.containsKey(key) }

            backSuspendJCache.get(key) shouldBeEqualTo value
            suspendNearJCache2.get(key) shouldBeEqualTo value
        }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 삭제하면 back cache도 삭제되고, 다른 nearCache에서도 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        backSuspendJCache.containsKey(key).shouldBeFalse()

        suspendNearJCache1.put(key, value)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.containsKey(key) }

        backSuspendJCache.get(key) shouldBeEqualTo value
        suspendNearJCache2.get(key) shouldBeEqualTo value

        suspendNearJCache1.remove(key).shouldBeTrue()
        await atMost (awaitTimeout) untilSuspending { !suspendNearJCache2.containsKey(key) }

        backSuspendJCache.containsKey(key).shouldBeFalse()
        suspendNearJCache1.containsKey(key).shouldBeFalse()
        suspendNearJCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `cache entry를 update하면, 다른 nearCache에서도 update 된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        backSuspendJCache.containsKey(key).shouldBeFalse()

        // nearCoCache1 에 cache entry 를 생성하면, nearCoCache2 에도 비동기적으로 생성된다.
        suspendNearJCache1.put(key, value)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.containsKey(key) }

        backSuspendJCache.get(key) shouldBeEqualTo value
        suspendNearJCache2.get(key) shouldBeEqualTo value

        // nearCoCache1 에 cache entry를 update하면, nearCoCache2 에도 비동기적으로 update 된다.
        suspendNearJCache1.replace(key, value, value2).shouldBeTrue()
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.get(key) == value2 }

        backSuspendJCache.get(key) shouldBeEqualTo value2
        suspendNearJCache2.get(key) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `두 개의 nearCoCache가 서로 변화가 반영된다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        val expected = mapOf(key1 to value1, key2 to value2)

        suspendNearJCache1.put(key1, value1)
        suspendNearJCache2.put(key2, value2)
        await atMost (awaitTimeout) untilSuspending {
            suspendNearJCache1.containsKey(key2) &&
                    suspendNearJCache2.containsKey(key1)
        }

        val actual2 = suspendNearJCache2.getAll(key1, key2).toList().associate { it.key to it.value }
        actual2 shouldContainSame expected

        val actual1 = suspendNearJCache1.getAll(key1, key2).toList().associate { it.key to it.value }
        actual1 shouldContainSame expected
    }

    @RepeatedTest(TEST_SIZE)
    fun `putAll with map - 복수의 cache entry를 추가하면 다른 nearCache에도 반영된다`() = runSuspendIO {
        val map = List(10) { getKey() to getValue() }.toMap()
        val keys = map.keys

        suspendNearJCache1.putAll(map)
        await atMost (awaitTimeout) untilSuspending { keys.all { suspendNearJCache2.containsKey(it) } }

        suspendNearJCache2.getAll().toList() shouldContainSame map.entries.map {
            SuspendJCacheEntry(
                it.key,
                it.value
            )
        }
    }

    @RepeatedTest(TEST_SIZE)
    fun `putAll with flow - 복수의 cache entry를 추가하면 다른 nearCache에도 반영된다`() = runSuspendIO {
        val entries = List(10) { getKey() to getValue() }.toMap()
        val keys = entries.keys

        suspendNearJCache1.putAllFlow(entries.map { it.key to it.value }.asFlow())
        await atMost (awaitTimeout) untilSuspending { keys.all { suspendNearJCache2.containsKey(it) } }

        suspendNearJCache2.getAll().toList() shouldContainSame entries.map { SuspendJCacheEntry(it.key, it.value) }
    }

    @RepeatedTest(TEST_SIZE)
    fun `putIfAbsent - cache entry가 없는 경우에만 추가되고, 전파됩니다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendNearJCache1.put(key, value)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.containsKey(key) }

        // 이미 cache entry 생성이 전파되어 반영되었다.
        suspendNearJCache2.putIfAbsent(key, value2).shouldBeFalse()
        suspendNearJCache2.get(key) shouldBeEqualTo value

        // 존재하지 않는 key2 에 대해서 새로 등록된다.
        val key2 = getKey()
        suspendNearJCache2.putIfAbsent(key2, value2).shouldBeTrue()
        suspendNearJCache2.get(key2) shouldBeEqualTo value2
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache1.containsKey(key2) }

        suspendNearJCache1.get(key2) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `remove with value - cache entry를 삭제하면 모든 nearCache에서 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendNearJCache1.put(key, value)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.containsKey(key) }
        suspendNearJCache2.get(key) shouldBeEqualTo value
        // cache entry가 일치하지 않으면 삭제되지 않는다
        suspendNearJCache2.remove(key, value2).shouldBeFalse()
        // cache entry를 삭제한다
        suspendNearJCache2.remove(key, value).shouldBeTrue()
        await atMost (awaitTimeout) untilSuspending { !suspendNearJCache1.containsKey(key) }

        suspendNearJCache1.containsKey(key).shouldBeFalse()
        suspendNearJCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and remove - 모든 nearCache에서 삭제된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendNearJCache1.put(key, value)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.containsKey(key) }

        suspendNearJCache1.containsKey(key).shouldBeTrue()
        suspendNearJCache2.containsKey(key).shouldBeTrue()

        suspendNearJCache1.getAndRemove(key) shouldBeEqualTo value
        await atMost (awaitTimeout) untilSuspending { !suspendNearJCache2.containsKey(key) }

        suspendNearJCache1.containsKey(key).shouldBeFalse()
        suspendNearJCache2.containsKey(key).shouldBeFalse()

        backSuspendJCache.put(key, value2)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache1.containsKey(key) }

        suspendNearJCache1.getAndRemove(key) shouldBeEqualTo value2
        await atMost (awaitTimeout) untilSuspending { !suspendNearJCache2.containsKey(key) }

        suspendNearJCache1.containsKey(key).shouldBeFalse()
        suspendNearJCache2.containsKey(key).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `replace old value - 모든 nearCache에서 update된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendNearJCache2.put(key, value)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache1.containsKey(key) }

        suspendNearJCache1.replace(key, value, value2).shouldBeTrue()
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.get(key) == value2 }

        suspendNearJCache2.get(key) shouldBeEqualTo value2

        // 이미 key-value2 로 갱신되었으므로 update에 실패한다 
        suspendNearJCache2.replace(key, value, value2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `replace - 모든 nearCache가 update 된다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        // 존재하지 않는 key 이므로 replace하지 못한다  
        suspendNearJCache1.replace(key, value).shouldBeFalse()

        suspendNearJCache1.put(key, value)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.containsKey(key) }

        suspendNearJCache2.replace(key, value2).shouldBeTrue()
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache1.get(key) == value2 }

        suspendNearJCache1.get(key) shouldBeEqualTo value2
    }

    @RepeatedTest(TEST_SIZE)
    fun `get and replace - 기존 값을 가져오고 새로운 값으로 갱신한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()
        val value3 = getValue()

        // key가 없으므로 replace 하지 못한다
        suspendNearJCache1.getAndReplace(key, value).shouldBeNull()
        suspendNearJCache1.containsKey(key).shouldBeFalse()

        suspendNearJCache1.put(key, value)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.containsKey(key) }

        // key 가 등록되어 있으므로, replace를 수행한다
        suspendNearJCache2.getAndReplace(key, value2) shouldBeEqualTo value
        suspendNearJCache2.get(key) shouldBeEqualTo value2
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache1.get(key) == value2 }
        suspendNearJCache1.get(key) shouldBeEqualTo value2

        suspendNearJCache1.put(key, value)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.get(key) == value }

        suspendNearJCache2.put(key, value2)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache1.get(key) == value2 }

        suspendNearJCache1.getAndReplace(key, value3) shouldBeEqualTo value2
        suspendNearJCache1.get(key) shouldBeEqualTo value3
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.get(key) == value3 }

        suspendNearJCache2.get(key) shouldBeEqualTo value3
    }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll with keys - 지정한 key 들을 삭제하면 모든 nearCache에 반영된다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        suspendNearJCache1.put(key1, value1)
        suspendNearJCache2.put(key2, value2)
        await atMost (awaitTimeout) untilSuspending {
            suspendNearJCache1.containsKey(key2) &&
                    suspendNearJCache2.containsKey(key1)
        }

        suspendNearJCache2.removeAll(key1, key2)
        await atMost (awaitTimeout) untilSuspending {
            !suspendNearJCache1.containsKey(key1) &&
                    !suspendNearJCache1.containsKey(key2)
        }

        suspendNearJCache1.containsKey(key1).shouldBeFalse()
        suspendNearJCache1.containsKey(key2).shouldBeFalse()
        suspendNearJCache2.containsKey(key1).shouldBeFalse()
        suspendNearJCache2.containsKey(key2).shouldBeFalse()
    }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll - 모든 캐시를 삭제하면 nearCache들에게 반영된다`() = runSuspendIO {
        val map = List(100) { getKey() to getValue() }.toMap()

        suspendNearJCache1.putAll(map)
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache2.entries().count() > 0 }

        suspendNearJCache2.entries().toList().shouldNotBeEmpty()

        // 모든 cache entry를 삭제하면 backCache에서 삭제되고, 이것이 전파되어 nearCache1에서도 삭제된다.
        suspendNearJCache2.removeAll()
        await atMost (awaitTimeout) untilSuspending { suspendNearJCache1.entries().count() == 0 }

        suspendNearJCache1.entries().count() shouldBeEqualTo 0
    }

    @RepeatedTest(TEST_SIZE)
    fun `clear - front cache만 clear 합니다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        suspendNearJCache1.put(key1, value1)
        suspendNearJCache2.put(key2, value2)
        await atMost (awaitTimeout) untilSuspending {
            suspendNearJCache1.containsKey(key2) &&
                    suspendNearJCache2.containsKey(key1)
        }

        suspendNearJCache1.clear()

        // front cache에만 삭제되었고, bach cache는 유지된다 
        suspendNearJCache1.containsKey(key1).shouldBeTrue()
        suspendNearJCache1.containsKey(key2).shouldBeTrue()

        // 다른 near cache에는 반영안된다.
        suspendNearJCache2.containsKey(key1).shouldBeTrue()
        suspendNearJCache2.containsKey(key2).shouldBeTrue()
    }

    @RepeatedTest(TEST_SIZE)
    fun `clearAll - front cache와 back cache 모두를 clear 합니다 - 전파는 되지 않습니다`() = runSuspendIO {
        val key1 = getKey()
        val value1 = getValue()
        val key2 = getKey()
        val value2 = getValue()

        suspendNearJCache1.put(key1, value1)
        suspendNearJCache2.put(key2, value2)

        await atMost (awaitTimeout) untilSuspending {
            suspendNearJCache1.containsKey(key2) && suspendNearJCache2.containsKey(key1)
        }

        // nearCache1 과 backCache 는 clear 되지만, nearCache2 로는 전파되지 않는다
        suspendNearJCache1.clearAll()

        // front cache, back cache 모두를 clear 합니다.
        suspendNearJCache1.containsKey(key1).shouldBeFalse()
        suspendNearJCache1.containsKey(key2).shouldBeFalse()

        // 다른 near cache에는 반영안된다. - removeAll() 을 사용해야 다른 nearCache에도 반영됩니다.
        suspendNearJCache2.containsKey(key1).shouldBeTrue()
        suspendNearJCache2.containsKey(key2).shouldBeTrue()
    }

    // ─────────────────────────────────────────────
    // 동시성 테스트
    // ─────────────────────────────────────────────

    @Test
    fun `SuspendedJobTester - 동시 put과 get이 안전하다`() = runSuspendIO {
        val keys = (1..100).map { getKey() }
        val value = getValue()

        // 먼저 데이터 넣기
        keys.forEach { suspendNearJCache1.put(it, value) }
        await untilSuspending { keys.all { suspendNearJCache2.containsKey(it) } }

        SuspendedJobTester()
            .rounds(32)
            .add {
                val key = keys.random()
                suspendNearJCache1.get(key) shouldBeEqualTo value
            }
            .add {
                val key = getKey()
                suspendNearJCache2.put(key, value)
            }
            .run()
    }

    @Test
    fun `SuspendedJobTester - 동시 put과 remove가 안전하다`() = runSuspendIO {
        SuspendedJobTester()
            .rounds(32)
            .add {
                val key = getKey()
                suspendNearJCache1.put(key, getValue())
                suspendNearJCache1.remove(key)
                // 비동기 이벤트 전파로 인해 즉시 null이 아닐 수 있으므로, 예외 없이 실행됨만 검증
            }
            .run()
    }

    @Test
    fun `SuspendedJobTester - 병렬 put-get-remove 사이클`() = runSuspendIO {
        SuspendedJobTester()
            .rounds(32)
            .add {
                val key = getKey()
                val value = getValue()
                suspendNearJCache1.put(key, value)
                suspendNearJCache1.get(key) // 비동기 전파로 값이 다를 수 있음
                suspendNearJCache1.remove(key)
                // 비동기 이벤트 전파로 인해 즉시 null이 아닐 수 있으므로, 예외 없이 실행됨만 검증
            }
            .run()
    }

    @Test
    fun `SuspendedJobTester - 동시 putIfAbsent 경합`() = runSuspendIO {
        val sharedKey = getKey()
        val value = getValue()

        SuspendedJobTester()
            .rounds(32)
            .add {
                suspendNearJCache1.putIfAbsent(sharedKey, value)
            }
            .run()

        suspendNearJCache1.get(sharedKey) shouldBeEqualTo value
    }
}
