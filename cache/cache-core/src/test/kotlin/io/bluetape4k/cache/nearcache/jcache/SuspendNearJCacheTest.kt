package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.CaffeineSuspendJCache
import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.Duration

@Execution(ExecutionMode.SAME_THREAD)
class SuspendNearJCacheTest {

    companion object: KLoggingChannel() {
        private fun randomKey(): String = Fakers.randomUuid().encodeBase62()
        private fun randomValue(): String = Fakers.randomString(64, 256)
    }

    private fun newSuspendJCache(): SuspendJCache<String, Any> =
        CaffeineSuspendJCache {
            expireAfterWrite(Duration.ofSeconds(60))
            maximumSize(10_000)
        }

    private lateinit var frontCache: SuspendJCache<String, Any>
    private lateinit var backCache: SuspendJCache<String, Any>
    private lateinit var nearCache: SuspendNearJCache<String, Any>

    @BeforeEach
    fun setup() = runSuspendIO {
        frontCache = newSuspendJCache()
        backCache = newSuspendJCache()
        nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)
        nearCache.clearAll()
    }

    @Test
    fun `create SuspendNearJCache withoutListener`() {
        nearCache.shouldNotBeNull()
        nearCache.isClosed().shouldBeFalse()
    }

    @Test
    fun `put and get - 값을 저장하고 조회한다`() = runSuspendIO {
        val key = randomKey()
        val value = randomValue()

        nearCache.put(key, value)

        nearCache.get(key) shouldBeEqualTo value
        nearCache.containsKey(key).shouldBeTrue()
    }

    @Test
    fun `getDeeply - front에 없으면 back에서 조회한다`() = runSuspendIO {
        val key = randomKey()
        val value = randomValue()

        backCache.put(key, value)

        // front에는 없고 back에는 있는 상태
        frontCache.get(key).shouldBeNull()

        val result = nearCache.getDeeply(key)
        result shouldBeEqualTo value

        // getDeeply 후 front에도 채워져야 함
        frontCache.get(key) shouldBeEqualTo value
    }

    @Test
    fun `get - 존재하지 않는 키는 null 반환`() = runSuspendIO {
        nearCache.get(randomKey()).shouldBeNull()
    }

    @Test
    fun `putAll and getAll - 여러 항목 저장 및 조회`() = runSuspendIO {
        val entries = (1..5).associate { "key-$it" to "value-$it" as Any }
        nearCache.putAll(entries)

        entries.forEach { (k, v) ->
            nearCache.get(k) shouldBeEqualTo v
        }
    }

    @Test
    fun `putIfAbsent - 없을 때만 저장`() = runSuspendIO {
        val key = randomKey()
        val value1 = randomValue()
        val value2 = randomValue()

        nearCache.putIfAbsent(key, value1).shouldBeTrue()
        nearCache.putIfAbsent(key, value2).shouldBeFalse()

        nearCache.get(key) shouldBeEqualTo value1
    }

    @Test
    fun `remove - 키를 삭제한다`() = runSuspendIO {
        val key = randomKey()
        val value = randomValue()

        nearCache.put(key, value)
        nearCache.containsKey(key).shouldBeTrue()

        nearCache.remove(key).shouldBeTrue()
        nearCache.get(key).shouldBeNull()
    }

    @Test
    fun `remove - 존재하지 않는 키 삭제는 false 반환`() = runSuspendIO {
        nearCache.remove(randomKey()).shouldBeFalse()
    }

    @Test
    fun `remove with oldValue - 값이 일치할 때만 삭제`() = runSuspendIO {
        val key = randomKey()
        val value = randomValue()
        val wrongValue = randomValue()

        nearCache.put(key, value)

        nearCache.remove(key, wrongValue).shouldBeFalse()
        nearCache.get(key) shouldBeEqualTo value

        nearCache.remove(key, value).shouldBeTrue()
        nearCache.get(key).shouldBeNull()
    }

    @Test
    fun `removeAll with keys - 특정 키들을 삭제한다`() = runSuspendIO {
        val entries = (1..5).associate { "key-$it" to "value-$it" as Any }
        nearCache.putAll(entries)

        nearCache.removeAll("key-1", "key-2", "key-3")

        nearCache.get("key-1").shouldBeNull()
        nearCache.get("key-2").shouldBeNull()
        nearCache.get("key-3").shouldBeNull()
        nearCache.get("key-4") shouldBeEqualTo "value-4"
        nearCache.get("key-5") shouldBeEqualTo "value-5"
    }

    @Test
    fun `removeAll - 모든 항목 삭제`() = runSuspendIO {
        val entries = (1..5).associate { "key-$it" to "value-$it" as Any }
        nearCache.putAll(entries)

        nearCache.removeAll()

        entries.keys.forEach { k ->
            nearCache.containsKey(k).shouldBeFalse()
        }
    }

    @Test
    fun `clear - front cache만 비운다`() = runSuspendIO {
        val key = randomKey()
        val value = randomValue()

        nearCache.put(key, value)
        nearCache.clear()

        // front는 비워지지만 back에는 남아있음
        backCache.containsKey(key).shouldBeTrue()
    }

    @Test
    fun `clearAll - front와 back 모두 비운다`() = runSuspendIO {
        val key = randomKey()
        val value = randomValue()

        nearCache.put(key, value)
        nearCache.clearAll()

        frontCache.get(key).shouldBeNull()
        backCache.get(key).shouldBeNull()
    }

    @Test
    fun `getAndPut - 이전 값을 반환하고 새 값을 저장한다`() = runSuspendIO {
        val key = randomKey()
        val value1 = randomValue()
        val value2 = randomValue()

        nearCache.put(key, value1)
        val old = nearCache.getAndPut(key, value2)

        old shouldBeEqualTo value1
        nearCache.get(key) shouldBeEqualTo value2
    }

    @Test
    fun `getAndRemove - 값을 조회하고 삭제한다`() = runSuspendIO {
        val key = randomKey()
        val value = randomValue()

        nearCache.put(key, value)
        val removed = nearCache.getAndRemove(key)

        removed shouldBeEqualTo value
        nearCache.get(key).shouldBeNull()
    }

    @Test
    fun `getAndRemove - 없는 키면 null 반환`() = runSuspendIO {
        nearCache.getAndRemove(randomKey()).shouldBeNull()
    }

    @Test
    fun `getAndReplace - 존재하는 경우 값을 교체`() = runSuspendIO {
        val key = randomKey()
        val value1 = randomValue()
        val value2 = randomValue()

        nearCache.put(key, value1)
        val old = nearCache.getAndReplace(key, value2)

        old shouldBeEqualTo value1
        nearCache.get(key) shouldBeEqualTo value2
    }

    @Test
    fun `replace with oldValue - 값이 일치할 때만 교체`() = runSuspendIO {
        val key = randomKey()
        val value1 = randomValue()
        val value2 = randomValue()
        val wrongValue = randomValue()

        nearCache.put(key, value1)

        nearCache.replace(key, wrongValue, value2).shouldBeFalse()
        nearCache.get(key) shouldBeEqualTo value1

        nearCache.replace(key, value1, value2).shouldBeTrue()
        nearCache.get(key) shouldBeEqualTo value2
    }

    @Test
    fun `replace - 존재할 때 값을 교체`() = runSuspendIO {
        val key = randomKey()
        val value1 = randomValue()
        val value2 = randomValue()

        nearCache.put(key, value1)
        nearCache.replace(key, value2).shouldBeTrue()
        nearCache.get(key) shouldBeEqualTo value2
    }

    @Test
    fun `replace - 존재하지 않으면 false`() = runSuspendIO {
        nearCache.replace(randomKey(), randomValue()).shouldBeFalse()
    }

    @Test
    fun `entries - flow로 모든 항목 조회`() = runSuspendIO {
        val entries = (1..5).associate { "key-$it" to "value-$it" as Any }
        nearCache.putAll(entries)

        val allEntries = nearCache.entries().toList()
        allEntries.shouldNotBeEmpty()
    }

    @Test
    fun `getAll flow - 모든 항목 조회`() = runSuspendIO {
        val entries = (1..5).associate { "key-$it" to "value-$it" as Any }
        nearCache.putAll(entries)

        val count = nearCache.getAll().count()
        count shouldBeEqualTo 5
    }

    @Test
    fun `getAll with keys - 지정 키 조회`() = runSuspendIO {
        val entries = (1..5).associate { "key-$it" to "value-$it" as Any }
        nearCache.putAll(entries)

        val keys = setOf("key-1", "key-3", "key-5")
        val results = nearCache.getAll(keys).map { it.key }.toList()
        results.toSet() shouldBeEqualTo keys
    }

    @Test
    fun `putAllFlow - Flow로 여러 항목 저장`() = runSuspendIO {
        val entries = (1..5).map { "key-$it" to "value-$it" as Any }

        nearCache.putAllFlow(
            kotlinx.coroutines.flow.flow {
                entries.forEach { emit(it) }
            }
        )

        entries.forEach { (k, v) ->
            nearCache.get(k) shouldBeEqualTo v
        }
    }

    @Test
    fun `close - front cache를 닫는다`() = runSuspendIO {
        nearCache.isClosed().shouldBeFalse()
        nearCache.close()
        nearCache.isClosed().shouldBeTrue()
    }
}
