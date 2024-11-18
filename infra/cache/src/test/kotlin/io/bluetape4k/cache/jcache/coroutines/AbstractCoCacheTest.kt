package io.bluetape4k.cache.jcache.coroutines

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class AbstractCoCacheTest {

    companion object: KLogging() {
        const val CACHE_ENTRY_SIZE = 100
        const val TEST_SIZE = 3

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String =
            Fakers.randomString(1024, 2048)
    }

    protected abstract val coCache: CoCache<String, Any>

    open fun getKey() = Fakers.randomUuid().encodeBase62()
    open fun getValue() = randomString()

    @BeforeEach
    fun setup() {
        runSuspendIO { coCache.clear() }
    }

    @AfterAll
    fun afterAll() {
        runBlocking { coCache.close() }
    }

    @Test
    fun `entries - get all cache entries by flow`() = runSuspendIO {
        coCache.clear()
        coCache.entries().map { it.key }.toList().size shouldBeEqualTo 0

        coCache.put(getKey(), getValue())
        coCache.put(getKey(), getValue())
        coCache.put(getKey(), getValue())

        val entries = coCache.entries().toList()
        entries.size shouldBeEqualTo 3
    }

    @Test
    fun `clear - clear all cache entries`() = runSuspendIO {
        coCache.put(getKey(), getValue())
        coCache.entries().map { it.key }.toList().size shouldBeEqualTo 1

        coCache.clear()
        coCache.entries().map { it.key }.toList().size shouldBeEqualTo 0
    }

    @Test
    fun `put - cache entry 추가`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        coCache.put(key, value)
        coCache.get(key) shouldBeEqualTo value
    }

    @Test
    fun `containsKey - 저장된 key가 존재하는지 검사`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        coCache.containsKey(key).shouldBeFalse()

        coCache.put(key, value)
        coCache.containsKey(key).shouldBeTrue()
    }

    @Test
    fun `get - 저장된 값 가져오기`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        coCache.put(key, value)

        coCache.get(key) shouldBeEqualTo value
    }

    @Test
    fun `getAll - 요청한 모든 cache entry 가져오기`() = runSuspendIO {
        repeat(CACHE_ENTRY_SIZE) {
            coCache.put(getKey(), getValue())
        }
        val entries = coCache.getAll().toList()
        entries.size shouldBeEqualTo CACHE_ENTRY_SIZE
    }

    @Test
    fun `getAll - with keys`() = runSuspendIO {
        val entries = List(CACHE_ENTRY_SIZE) {
            CoCacheEntry(getKey(), getValue()).apply {
                coCache.put(key, value)
            }
        }
        val keysToLoad = setOf(entries.first().key, entries[42].key, entries[51].key, entries.last().key)
        val loaded = coCache.getAll(keysToLoad).toList()
        loaded.map { it.key }.toSet() shouldBeEqualTo keysToLoad
    }

    @Test
    fun `getAndPut - 기존 값을 가져오고 새로운 값으로 저장한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        coCache.getAndPut(key, value).shouldBeNull()
        coCache.getAndPut(key, value2) shouldBeEqualTo value
    }

    @Test
    fun `getAndRemove - 기존 값을 가져오고 삭제한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        coCache.getAndRemove(key).shouldBeNull()

        coCache.put(key, value)
        coCache.getAndRemove(key) shouldBeEqualTo value
    }

    @Test
    fun `getAndReplace - 기존 값을 가져오고, 새로운 값으로 대체한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        // 기존에 등록된 값이 없으므로 replace도 하지 않는다.
        coCache.getAndReplace(key, value).shouldBeNull()
        coCache.containsKey(key).shouldBeFalse()

        coCache.put(key, value)

        coCache.getAndReplace(key, value2) shouldBeEqualTo value
        coCache.get(key) shouldBeEqualTo value2
    }

    @Test
    fun `put - cache entry를 추가한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        coCache.containsKey(key).shouldBeFalse()
        coCache.put(key, value)
        coCache.containsKey(key).shouldBeTrue()
    }

    @Test
    fun `putAll - 모든 entry를 추가합니다`() = runSuspendIO {
        val entries = List(CACHE_ENTRY_SIZE) { getKey() to getValue() }.toMap()

        coCache.entries().toList().size shouldBeEqualTo 0
        coCache.putAll(entries)
        coCache.entries().toList().size shouldBeEqualTo CACHE_ENTRY_SIZE
    }

    @Test
    fun `putAllFlow - flow 를 cache entry로 모두 추가한다`() = runSuspendIO {
        val entries = flow {
            repeat(CACHE_ENTRY_SIZE) {
                emit(getKey() to getValue())
            }
        }
        coCache.entries().toList().size shouldBeEqualTo 0
        coCache.putAllFlow(entries)
        coCache.entries().toList().size shouldBeEqualTo CACHE_ENTRY_SIZE
    }


    @Test
    fun `putIfAbsent - 기존에 값이 없을 때에만 새로 추가한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        coCache.putIfAbsent(key, value).shouldBeTrue()
        coCache.putIfAbsent(key, value2).shouldBeFalse()
        coCache.get(key) shouldBeEqualTo value
    }

    @Test
    fun `remove - 해당 Cache entry를 제거한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        coCache.remove(key).shouldBeFalse()

        coCache.put(key, value)
        coCache.remove(key).shouldBeTrue()
        coCache.containsKey(key).shouldBeFalse()
    }

    @Test
    fun `remove with oldValue - 지정한 값을 가진 경우에만 제거한다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        coCache.remove(key, value).shouldBeFalse()

        coCache.put(key, value)

        coCache.remove(key, value2).shouldBeFalse()
        coCache.containsKey(key).shouldBeTrue()

        coCache.remove(key, value).shouldBeTrue()
        coCache.containsKey(key).shouldBeFalse()
    }

    @Test
    fun `removeAll - 모든 cache entry를 삭제한다`() = runSuspendIO {
        repeat(CACHE_ENTRY_SIZE) {
            CoCacheEntry(getKey(), getValue()).apply {
                coCache.put(key, value)
            }
        }
        coCache.entries().toList() shouldHaveSize CACHE_ENTRY_SIZE

        coCache.removeAll()

        coCache.entries().toList().shouldBeEmpty()
    }

    @Test
    fun `removeAll with keys - 지정한 key 값들에 해당하는 cache entry를 삭제한다`() = runSuspendIO {
        val entries = List(CACHE_ENTRY_SIZE) {
            CoCacheEntry(getKey(), getValue()).apply {
                coCache.put(key, value)
            }
        }
        coCache.entries().toList().size shouldBeEqualTo CACHE_ENTRY_SIZE

        val keysToRemove = setOf(entries.first().key, entries[42].key, entries.last().key)
        coCache.removeAll(keysToRemove)

        coCache.entries().toList().size shouldBeEqualTo CACHE_ENTRY_SIZE - keysToRemove.size
    }

    @Test
    fun `removeAll with vararg keys`() = runSuspendIO {
        val entries = List(CACHE_ENTRY_SIZE) {
            CoCacheEntry(getKey(), getValue()).apply {
                coCache.put(key, value)
            }
        }
        coCache.entries().toList().size shouldBeEqualTo CACHE_ENTRY_SIZE

        val keysToRemove = setOf(entries.first().key, entries[42].key, entries.last().key)
        coCache.removeAll(*keysToRemove.toTypedArray())

        coCache.entries().toList().size shouldBeEqualTo CACHE_ENTRY_SIZE - keysToRemove.size
    }

    @Test
    fun `replace - 기존 cache key의 값을 변경합니다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        // 존재하지 않은 key 이다 
        coCache.replace(key, value).shouldBeFalse()

        coCache.put(key, value)
        coCache.get(key) shouldBeEqualTo value

        coCache.replace(key, value2).shouldBeTrue()
        coCache.get(key) shouldBeEqualTo value2
    }

    @Test
    fun `replace - 기존 cache entry의 값을 변경합니다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()
        val value2 = getValue()

        // 존재하지 않은 key 이다
        coCache.replace(key, value, value2).shouldBeFalse()

        coCache.put(key, value)
        coCache.get(key) shouldBeEqualTo value

        coCache.replace(key, value, value2).shouldBeTrue()
        coCache.get(key) shouldBeEqualTo value2
    }
}
