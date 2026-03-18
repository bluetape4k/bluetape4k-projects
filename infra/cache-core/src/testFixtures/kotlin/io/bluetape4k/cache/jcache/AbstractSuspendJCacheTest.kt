package io.bluetape4k.cache.jcache

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class AbstractSuspendJCacheTest {

    companion object: KLoggingChannel() {
        const val CACHE_ENTRY_SIZE = 100
        const val TEST_SIZE = 3

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String =
            Fakers.randomString(1024, 2048)
    }

    protected abstract val suspendJCache: SuspendJCache<String, Any>

    open fun getKey() = Fakers.randomUuid().encodeBase62()
    open fun getValue() = randomString()

    @BeforeEach
    fun setup() {
        runSuspendIO { suspendJCache.clear() }
    }

    @AfterAll
    fun afterAll() {
        runBlocking { suspendJCache.close() }
    }

    @Test
    fun `entries - get all cache entries by flow`() = runSuspendIO {
        suspendJCache.clear()
        suspendJCache.entries().map { it.key }.count() shouldBeEqualTo 0

        suspendJCache.put(getKey(), getValue())
        suspendJCache.put(getKey(), getValue())
        suspendJCache.put(getKey(), getValue())

        suspendJCache.entries().count() shouldBeEqualTo 3
    }

    @Test
    fun `clear - clear all cache entries`() = runSuspendIO {
        suspendJCache.put(getKey(), getValue())
        suspendJCache.entries().map { it.key }.count() shouldBeEqualTo 1

        suspendJCache.clear()
        suspendJCache.entries().map { it.key }.count() shouldBeEqualTo 0
    }

    @Test
    fun `put - cache entry 추가`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        suspendJCache.put(key, value)
        suspendJCache.get(key) shouldBeEqualTo value
    }

    @Test
    fun `containsKey - 저장된 key가 존재하는지 검사`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        suspendJCache.containsKey(key).shouldBeFalse()

        suspendJCache.put(key, value)
        suspendJCache.containsKey(key).shouldBeTrue()
    }

    @Test
    fun `get - 저장된 값 가져오기`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        suspendJCache.put(key, value)

        suspendJCache.get(key) shouldBeEqualTo value
    }

    @Test
    fun `getAll - 요청한 모든 cache entry 가져오기`() = runSuspendIO {
        repeat(CACHE_ENTRY_SIZE) {
            suspendJCache.put(getKey(), getValue())
        }
        suspendJCache.getAll().count() shouldBeEqualTo CACHE_ENTRY_SIZE
    }

    @Test
    fun `getAll - with keys`() = runSuspendIO {
        val entries = List(CACHE_ENTRY_SIZE) {
            SuspendJCacheEntry(getKey(), getValue()).apply {
                suspendJCache.put(key, value)
            }
        }
        val keysToLoad = setOf(entries.first().key, entries[42].key, entries[51].key, entries.last().key)
        val loaded = suspendJCache.getAll(keysToLoad)
        loaded.map { it.key }.toSet() shouldBeEqualTo keysToLoad
    }

    @Test
    fun `getAndPut - 기존 값을 가져오고 새로운 값으로 저장한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendJCache.getAndPut(key, value).shouldBeNull()
        suspendJCache.getAndPut(key, value2) shouldBeEqualTo value
    }

    @Test
    fun `getAndRemove - 기존 값을 가져오고 삭제한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        suspendJCache.getAndRemove(key).shouldBeNull()

        suspendJCache.put(key, value)
        suspendJCache.getAndRemove(key) shouldBeEqualTo value
    }

    @Test
    fun `getAndReplace - 기존 값을 가져오고, 새로운 값으로 대체한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        // 기존에 등록된 값이 없으므로 replace도 하지 않는다.
        suspendJCache.getAndReplace(key, value).shouldBeNull()
        suspendJCache.containsKey(key).shouldBeFalse()

        suspendJCache.put(key, value)

        suspendJCache.getAndReplace(key, value2) shouldBeEqualTo value
        suspendJCache.get(key) shouldBeEqualTo value2
    }

    @Test
    fun `put - cache entry를 추가한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        suspendJCache.containsKey(key).shouldBeFalse()
        suspendJCache.put(key, value)
        suspendJCache.containsKey(key).shouldBeTrue()
    }

    @Test
    fun `putAll - 모든 entry를 추가합니다`() = runSuspendIO {
        val entries = List(CACHE_ENTRY_SIZE) { getKey() to getValue() }.toMap()

        suspendJCache.entries().count() shouldBeEqualTo 0
        suspendJCache.putAll(entries)
        suspendJCache.entries().count() shouldBeEqualTo CACHE_ENTRY_SIZE
    }

    @Test
    fun `putAllFlow - flow 를 cache entry로 모두 추가한다`() = runSuspendIO {
        val entries = flow {
            repeat(CACHE_ENTRY_SIZE) {
                emit(getKey() to getValue())
            }
        }
        suspendJCache.entries().count() shouldBeEqualTo 0
        suspendJCache.putAllFlow(entries)
        suspendJCache.entries().count() shouldBeEqualTo CACHE_ENTRY_SIZE
    }


    @Test
    fun `putIfAbsent - 기존에 값이 없을 때에만 새로 추가한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendJCache.putIfAbsent(key, value).shouldBeTrue()
        suspendJCache.putIfAbsent(key, value2).shouldBeFalse()
        suspendJCache.get(key) shouldBeEqualTo value
    }

    @Test
    fun `remove - 해당 Cache entry를 제거한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        suspendJCache.remove(key).shouldBeFalse()

        suspendJCache.put(key, value)
        suspendJCache.remove(key).shouldBeTrue()
        suspendJCache.containsKey(key).shouldBeFalse()
    }

    @Test
    fun `remove with oldValue - 지정한 값을 가진 경우에만 제거한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        suspendJCache.remove(key, value).shouldBeFalse()

        suspendJCache.put(key, value)

        suspendJCache.remove(key, value2).shouldBeFalse()
        suspendJCache.containsKey(key).shouldBeTrue()

        suspendJCache.remove(key, value).shouldBeTrue()
        suspendJCache.containsKey(key).shouldBeFalse()
    }

    @Test
    fun `removeAll - 모든 cache entry를 삭제한다`() = runSuspendIO {
        repeat(CACHE_ENTRY_SIZE) {
            SuspendJCacheEntry(getKey(), getValue()).apply {
                suspendJCache.put(key, value)
            }
        }
        suspendJCache.entries().count() shouldBeEqualTo CACHE_ENTRY_SIZE

        suspendJCache.removeAll()

        suspendJCache.entries().count() shouldBeEqualTo 0
    }

    @Test
    fun `removeAll with keys - 지정한 key 값들에 해당하는 cache entry를 삭제한다`() = runSuspendIO {
        val entries = List(CACHE_ENTRY_SIZE) {
            SuspendJCacheEntry(getKey(), getValue()).apply {
                suspendJCache.put(key, value)
            }
        }
        suspendJCache.entries().count() shouldBeEqualTo CACHE_ENTRY_SIZE

        val keysToRemove = setOf(entries.first().key, entries[42].key, entries.last().key)
        suspendJCache.removeAll(keysToRemove)

        suspendJCache.entries().count() shouldBeEqualTo CACHE_ENTRY_SIZE - keysToRemove.size
    }

    @Test
    fun `removeAll with vararg keys`() = runSuspendIO {
        val entries = List(CACHE_ENTRY_SIZE) {
            SuspendJCacheEntry(getKey(), getValue()).apply {
                suspendJCache.put(key, value)
            }
        }
        suspendJCache.entries().count() shouldBeEqualTo CACHE_ENTRY_SIZE

        val keysToRemove = setOf(entries.first().key, entries[42].key, entries.last().key)
        suspendJCache.removeAll(*keysToRemove.toTypedArray())

        suspendJCache.entries().count() shouldBeEqualTo CACHE_ENTRY_SIZE - keysToRemove.size
    }

    @Test
    fun `replace - 기존 cache key의 값을 변경합니다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        // 존재하지 않은 key 이다 
        suspendJCache.replace(key, value).shouldBeFalse()

        suspendJCache.put(key, value)
        suspendJCache.get(key) shouldBeEqualTo value

        suspendJCache.replace(key, value2).shouldBeTrue()
        suspendJCache.get(key) shouldBeEqualTo value2
    }

    @Test
    fun `replace - 기존 cache entry의 값을 변경합니다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        // 존재하지 않은 key 이다
        suspendJCache.replace(key, value, value2).shouldBeFalse()

        suspendJCache.put(key, value)
        suspendJCache.get(key) shouldBeEqualTo value

        suspendJCache.replace(key, value, value2).shouldBeTrue()
        suspendJCache.get(key) shouldBeEqualTo value2
    }
}
