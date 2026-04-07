package io.bluetape4k.cache.nearcache

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

/**
 * [SuspendNearCacheOperations] 공통 테스트 추상 클래스.
 *
 * 모든 Suspend NearCache 구현체가 이 클래스를 상속하여
 * 동일한 테스트 시나리오를 검증합니다.
 *
 * @param V 캐시 값 타입
 */
@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractSuspendNearCacheOperationsTest<V: Any> {
    companion object: KLogging() {
        private const val TEST_SIZE = 3

        @JvmStatic
        protected fun randomKey(): String = "key-" + Fakers.randomString(8, 16)
    }

    /**
     * 테스트용 SuspendNearCache 인스턴스를 생성합니다.
     */
    abstract fun createCache(): SuspendNearCacheOperations<V>

    /**
     * 테스트에 사용할 샘플 값을 반환합니다.
     */
    abstract fun sampleValue(): V

    /**
     * [sampleValue]와 다른 값을 반환합니다.
     */
    abstract fun anotherValue(): V

    private val cache: SuspendNearCacheOperations<V> by lazy { createCache() }

    @AfterEach
    fun cleanup() =
        runSuspendIO {
            if (!cache.isClosed) {
                cache.clearAll()
            }
        }

    @Test
    fun `get - cache miss returns null`() =
        runSuspendIO {
            cache.get(randomKey()).shouldBeNull()
        }

    @RepeatedTest(TEST_SIZE)
    fun `put and get - round trip`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()

            cache.put(key, value)
            cache.get(key) shouldBeEqualTo value
        }

    @RepeatedTest(TEST_SIZE)
    fun `getAll - batch read`() =
        runSuspendIO {
            val entries = (1..5).associate { randomKey() to sampleValue() }
            cache.putAll(entries)

            val result = cache.getAll(entries.keys)
            result shouldContainSame entries
        }

    @RepeatedTest(TEST_SIZE)
    fun `putIfAbsent - returns null on success, existing value on failure`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()
            val anotherValue = anotherValue()

            // 키가 없으므로 저장 성공 → null 반환
            cache.putIfAbsent(key, value).shouldBeNull()
            cache.get(key) shouldBeEqualTo value

            // 이미 존재하므로 저장 실패 → 기존 값 반환
            val existing = cache.putIfAbsent(key, anotherValue)
            existing.shouldNotBeNull()
            existing shouldBeEqualTo value

            // 값은 변경되지 않음
            cache.get(key) shouldBeEqualTo value
        }

    @RepeatedTest(TEST_SIZE)
    fun `replace - existing key only`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()
            val newValue = anotherValue()

            // 키가 없으므로 replace 실패
            cache.replace(key, value).shouldBeFalse()

            cache.put(key, value)
            cache.replace(key, newValue).shouldBeTrue()
            cache.get(key) shouldBeEqualTo newValue
        }

    @RepeatedTest(TEST_SIZE)
    fun `replace - with oldValue check`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()
            val newValue = anotherValue()

            cache.put(key, value)

            cache.replace(key, value, newValue).shouldBeTrue()
            cache.get(key) shouldBeEqualTo newValue

            cache.replace(key, value, newValue).shouldBeFalse()
        }

    @RepeatedTest(TEST_SIZE)
    fun `remove - removes existing key`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()

            cache.put(key, value)
            cache.containsKey(key).shouldBeTrue()

            cache.remove(key)
            cache.containsKey(key).shouldBeFalse()
            cache.get(key).shouldBeNull()
        }

    @RepeatedTest(TEST_SIZE)
    fun `removeAll - removes multiple keys`() =
        runSuspendIO {
            val entries = (1..5).associate { randomKey() to sampleValue() }
            cache.putAll(entries)

            cache.removeAll(entries.keys)

            entries.keys.forEach { key ->
                cache.get(key).shouldBeNull()
            }
        }

    @RepeatedTest(TEST_SIZE)
    fun `getAndRemove - returns value and removes`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()

            cache.getAndRemove(key).shouldBeNull()

            cache.put(key, value)
            cache.getAndRemove(key) shouldBeEqualTo value
            cache.get(key).shouldBeNull()
        }

    @RepeatedTest(TEST_SIZE)
    fun `getAndReplace - returns old value`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()
            val newValue = anotherValue()

            cache.getAndReplace(key, value).shouldBeNull()

            cache.put(key, value)
            cache.getAndReplace(key, newValue) shouldBeEqualTo value
            cache.get(key) shouldBeEqualTo newValue
        }

    @RepeatedTest(TEST_SIZE)
    fun `clearAll - empties both tiers`() =
        runSuspendIO {
            val entries = (1..5).associate { randomKey() to sampleValue() }
            cache.putAll(entries)

            cache.clearAll()

            entries.keys.forEach { key ->
                cache.get(key).shouldBeNull()
            }
        }

    @RepeatedTest(TEST_SIZE)
    fun `clearLocal - empties front only, back retained`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()

            cache.put(key, value)
            cache.clearLocal()

            cache.get(key) shouldBeEqualTo value
        }

    @RepeatedTest(TEST_SIZE)
    fun `local cache populated on get`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()

            cache.put(key, value)
            cache.clearLocal()

            cache.get(key) shouldBeEqualTo value
            (cache.localCacheSize() >= 1L).shouldBeTrue()
        }

    @Test
    fun `containsKey - checks both tiers`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()

            cache.containsKey(key).shouldBeFalse()

            cache.put(key, value)
            cache.containsKey(key).shouldBeTrue()

            cache.remove(key)
            cache.containsKey(key).shouldBeFalse()
        }

    @RepeatedTest(TEST_SIZE)
    fun `stats - hits and misses tracked`() =
        runSuspendIO {
            val key = randomKey()
            val value = sampleValue()

            cache.put(key, value)
            cache.clearLocal()

            // back hit
            cache.get(key)

            // back miss
            cache.get(randomKey())

            val stats = cache.stats()
            (stats.backHits >= 1L).shouldBeTrue()
            (stats.backMisses >= 1L).shouldBeTrue()
        }

    // ─────────────────────────────────────────────
    // 동시성 테스트
    // ─────────────────────────────────────────────

    @RepeatedTest(TEST_SIZE)
    fun `SuspendedJobTester - 동시 put과 get이 안전하다`() = runSuspendIO {
        val keys = (1..50).map { randomKey() }
        val value = sampleValue()
        keys.forEach { cache.put(it, value) }

        SuspendedJobTester()
            .workers(8)
            .rounds(32)
            .add {
                val key = keys.random()
                cache.get(key) shouldBeEqualTo value
            }
            .add {
                cache.put(randomKey(), sampleValue())
            }
            .run()
    }

    @RepeatedTest(TEST_SIZE)
    fun `SuspendedJobTester - 동시 put과 remove가 안전하다`() = runSuspendIO {
        SuspendedJobTester()
            .workers(8)
            .rounds(32)
            .add {
                val key = randomKey()
                cache.put(key, sampleValue())
                cache.remove(key)
                cache.get(key).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(TEST_SIZE)
    fun `SuspendedJobTester - 병렬 put-get-remove 사이클`() = runSuspendIO {
        SuspendedJobTester()
            .workers(8)
            .rounds(32)
            .add {
                val key = randomKey()
                val value = sampleValue()
                cache.put(key, value)
                cache.get(key) shouldBeEqualTo value
                cache.remove(key)
                cache.get(key).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(TEST_SIZE)
    fun `SuspendedJobTester - 동시 putIfAbsent 경합`() = runSuspendIO {
        val sharedKey = randomKey()
        val value = sampleValue()

        SuspendedJobTester()
            .workers(8)
            .rounds(16)
            .add {
                cache.putIfAbsent(sharedKey, value)
            }
            .run()

        cache.get(sharedKey) shouldBeEqualTo value
    }
}
