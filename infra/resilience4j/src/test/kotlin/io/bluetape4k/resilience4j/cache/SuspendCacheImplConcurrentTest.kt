package io.bluetape4k.resilience4j.cache

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

/**
 * [SuspendCache] 동시성 안전성 테스트입니다.
 *
 * 키별 Mutex 도입 전에는 동일 키에 대한 동시 cache miss 시 loader()가 여러 번 호출되는
 * race condition이 있었습니다. 이 테스트는 동시 요청에서 loader()가 한 번만 실행됨을 검증합니다.
 */
class SuspendCacheImplConcurrentTest {

    companion object: KLoggingChannel()

    private lateinit var cache: SuspendCache<String, String>

    @BeforeEach
    fun setup() {
        val jcache = CaffeineJCacheProvider.getJCache<String, String>("concurrent-test-${System.nanoTime()}")
        jcache.clear()
        cache = SuspendCache.of(jcache)
    }

    @Test
    fun `동일 키에 대한 동시 요청은 loader를 정확히 한 번만 실행한다`() = runSuspendTest {
        val loaderCallCount = AtomicInteger(0)
        val concurrency = 20
        val key = "same-key"

        // 20개 코루틴이 동시에 동일 키로 cache miss를 유발합니다.
        // Mutex가 없으면 모든 코루틴이 loader()를 호출해 최대 20번까지 실행됩니다.
        val results = withContext(Dispatchers.Default) {
            (1..concurrency).map {
                async {
                    cache.computeIfAbsent(key) {
                        loaderCallCount.incrementAndGet()
                        delay(10.milliseconds) // 동시성 경합을 유발할 지연
                        "loaded-value"
                    }
                }
            }.awaitAll()
        }

        // 모든 코루틴이 동일한 값을 받아야 합니다.
        results.distinct().size shouldBeEqualTo 1
        results.first() shouldBeEqualTo "loaded-value"

        // loader()는 정확히 한 번만 실행되어야 합니다.
        // Mutex 도입 전에는 동시 요청 수만큼 loader()가 호출될 수 있었습니다.
        loaderCallCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `서로 다른 키에 대한 동시 요청은 각 키별로 loader를 한 번씩 실행한다`() = runSuspendTest {
        val loaderCallCount = AtomicInteger(0)
        val keys = (1..10).map { "key-$it" }

        // 각 키에 대해 2개의 동시 요청을 보냅니다.
        val results = withContext(Dispatchers.Default) {
            keys.flatMap { key ->
                listOf(
                    async {
                        cache.computeIfAbsent(key) {
                            loaderCallCount.incrementAndGet()
                            delay(5.milliseconds)
                            "value-for-$key"
                        }
                    },
                    async {
                        cache.computeIfAbsent(key) {
                            loaderCallCount.incrementAndGet()
                            delay(5.milliseconds)
                            "value-for-$key"
                        }
                    }
                )
            }.awaitAll()
        }

        // 각 키당 loader()는 최대 한 번만 실행되어야 합니다.
        loaderCallCount.get() shouldBeLessOrEqualTo keys.size

        // 모든 결과가 올바른 값을 가져야 합니다.
        keys.forEach { key ->
            val expected = "value-for-$key"
            results.count { it == expected } shouldBeEqualTo 2
        }
    }

    @Test
    fun `캐시에 이미 값이 있을 때 동시 요청은 loader를 실행하지 않는다`() = runSuspendTest {
        val loaderCallCount = AtomicInteger(0)
        val key = "pre-cached-key"

        // 먼저 캐시를 채웁니다.
        cache.computeIfAbsent(key) {
            loaderCallCount.incrementAndGet()
            "pre-cached-value"
        }

        loaderCallCount.get() shouldBeEqualTo 1

        // 이미 캐시된 키에 대한 동시 요청은 loader를 호출하지 않아야 합니다.
        withContext(Dispatchers.Default) {
            (1..10).map {
                async {
                    cache.computeIfAbsent(key) {
                        loaderCallCount.incrementAndGet()
                        "should-not-load"
                    }
                }
            }.awaitAll()
        }

        // loader()는 최초 1번 이후 추가 호출이 없어야 합니다.
        loaderCallCount.get() shouldBeEqualTo 1
    }
}
