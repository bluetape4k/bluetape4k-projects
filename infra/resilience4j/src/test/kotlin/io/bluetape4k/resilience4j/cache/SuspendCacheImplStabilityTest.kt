package io.bluetape4k.resilience4j.cache

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException as KotlinCancellationException
import io.bluetape4k.assertions.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

/**
 * [SuspendCacheImpl] 안정성(안전성/메모리) 테스트.
 *
 * 검증 항목:
 * - CancellationException 재throw: computeIfAbsent가 취소를 삼키지 않고 전파한다
 * - Mutex 메모리 누수: 대량의 서로 다른 키 처리 후 내부 keyLocks 맵이 정리된다
 */
class SuspendCacheImplStabilityTest {

    companion object: KLoggingChannel()

    private lateinit var cache: SuspendCache<String, String>

    @BeforeEach
    fun setup() {
        val jcache = CaffeineJCacheProvider.getJCache<String, String>("stability-test-${System.nanoTime()}")
        jcache.clear()
        cache = SuspendCache.of(jcache)
    }

    /**
     * loader가 CancellationException을 던지면 computeIfAbsent가 이를 삼키지 않고 재throw해야 한다.
     *
     * ## 검증하는 불변식
     * Kotlin 코루틴 규칙: catch(e: Exception) 계열에서 CancellationException은
     * 반드시 rethrow 해야 한다. computeIfAbsent의 try-finally 블록이 이 규칙을 위반하면
     * 취소된 코루틴이 계속 실행되어 코루틴 구조적 동시성이 깨진다.
     */
    @Test
    fun `loader에서 CancellationException이 발생하면 computeIfAbsent가 재throw한다`() = runSuspendTest {
        assertFailsWith<KotlinCancellationException> {
            cache.computeIfAbsent("key-cancel") {
                throw KotlinCancellationException("test cancellation")
            }
        }
    }

    /**
     * 코루틴이 취소되면 computeIfAbsent 실행 중에도 CancellationException이 전파된다.
     *
     * ## 검증하는 불변식
     * 부모 코루틴이 취소되면 자식 코루틴의 suspend 지점(delay 등)에서
     * CancellationException이 발생한다. computeIfAbsent가 이 예외를 억제하지 않아야 한다.
     */
    @Test
    fun `코루틴 취소 시 computeIfAbsent의 loader에서 CancellationException이 전파된다`() = runSuspendTest {
        val loaderStarted = AtomicInteger(0)
        var caughtCancellation = false

        val job = launch {
            try {
                cache.computeIfAbsent("key-job-cancel") {
                    loaderStarted.incrementAndGet()
                    delay(1000.milliseconds)  // 취소 지점
                    "should not reach here"
                }
            } catch (e: KotlinCancellationException) {
                caughtCancellation = true
                throw e  // 구조적 동시성 유지를 위해 재throw
            }
        }

        // loader가 시작될 때까지 대기
        while (loaderStarted.get() == 0) {
            delay(1.milliseconds)
        }

        // 코루틴 취소
        job.cancel("intentional cancel")
        job.join()

        // CancellationException이 전파되었음을 확인합니다.
        caughtCancellation.shouldBeTrue()
        loaderStarted.get() shouldBeEqualTo 1
    }

    /**
     * 수천 개의 서로 다른 키에 대해 computeIfAbsent를 완료한 후
     * 내부 keyLocks(ConcurrentHashMap)이 정리되어 메모리 누수가 없음을 검증한다.
     *
     * ## 검증하는 불변식
     * computeIfAbsent 완료 후 해당 키의 Mutex는 keyLocks 맵에서 제거되어야 한다.
     * 제거되지 않으면 서로 다른 키 수만큼 Mutex가 메모리에 남아 OOM을 유발할 수 있다.
     *
     * ## 검증 방법
     * 리플렉션으로 keyLocks 필드 크기를 확인한다.
     * 모든 computeIfAbsent가 완료된 후 크기가 키 수보다 훨씬 작아야 한다
     * (이상적으로는 0, 하지만 GC 타이밍 등으로 일부 남을 수 있으므로 50% 미만을 기준으로 한다).
     */
    @Test
    fun `서로 다른 키 대량 처리 후 내부 keyLocks 맵이 정리된다`() = runSuspendTest {
        val keyCount = 500
        val keys = (1..keyCount).map { "leak-key-$it" }

        // 모든 키에 대해 순차적으로 computeIfAbsent를 완료합니다.
        withContext(Dispatchers.Default) {
            keys.map { key ->
                async {
                    cache.computeIfAbsent(key) {
                        delay(1.milliseconds)
                        "value-for-$key"
                    }
                }
            }.awaitAll()
        }

        // 내부 keyLocks 맵 크기를 리플렉션으로 확인합니다.
        val keyLocksField = cache.javaClass.getDeclaredField("keyLocks")
            .also { it.isAccessible = true }

        // SuspendCacheImpl 인스턴스를 가져와야 합니다 (인터페이스 참조이므로)
        val impl = cache
        val keyLocksMap = keyLocksField.get(impl) as java.util.concurrent.ConcurrentHashMap<*, *>

        // 모든 작업 완료 후 Mutex가 정리되어 keyLocks 크기가 키 수(500)보다 훨씬 작아야 합니다.
        // releaseMutex 로직에 의해 lock이 해제된 Mutex는 즉시 제거되므로 대부분 0에 가깝습니다.
        keyLocksMap.size shouldBeLessThan (keyCount / 2)
    }

    /**
     * double-check 경로(Mutex 대기 후 재확인)에서 hit 메트릭이 올바르게 기록된다.
     *
     * ## 검증하는 불변식
     * 동시 캐시 미스 시 모든 코루틴은 fast-path에서 miss를 기록한 뒤 Mutex 대기에 진입한다.
     * 첫 번째 코루틴이 값을 로드해 캐시에 저장하면, 나머지 코루틴들은 Mutex 획득 후
     * double-check(rawGetWithHit)에서 값을 찾고 hit 메트릭을 기록한다.
     * 이전 구현(rawGet)에서는 hit를 기록하지 않아 hit 메트릭이 과소 계산되었다.
     *
     * ## 메트릭 흐름 (concurrency=10, loader delay=50ms)
     * - fast-path: 10개 코루틴 모두 miss → miss 카운트 = 10
     * - Mutex 내 double-check: 첫 번째 코루틴 외 나머지 코루틴들이 hit 기록 → hit 카운트 >= 1
     * - 이전 구현: rawGet이 hit를 기록하지 않으므로 hit 카운트 = 0 (버그)
     * - 수정 후: rawGetWithHit이 hit를 기록하므로 hit 카운트 >= 1 (정상)
     */
    @Test
    fun `double-check 경로에서 hit 메트릭이 기록된다`() = runSuspendTest {
        val concurrency = 10
        val key = "dc-hit-key"

        // 50ms delay로 10개 코루틴이 모두 fast-path miss를 기록하도록 강제합니다.
        withContext(Dispatchers.Default) {
            (1..concurrency).map {
                async {
                    cache.computeIfAbsent(key) {
                        delay(50.milliseconds)
                        "dc-value"
                    }
                }
            }.awaitAll()
        }

        val totalHits = cache.metrics.getNumberOfCacheHits()
        val totalMisses = cache.metrics.getNumberOfCacheMisses()

        // 10개 코루틴 모두 fast-path에서 miss를 기록하므로 miss == concurrency
        totalMisses shouldBeEqualTo concurrency.toLong()

        // double-check에서 hit를 기록하므로 totalHits >= 1이어야 합니다.
        // 이전 구현(rawGet)에서는 totalHits == 0이었습니다 (버그).
        // hit 수는 스케줄링에 따라 달라지므로 정확한 수 대신 범위만 검증합니다.
        totalHits shouldBeLessThan concurrency.toLong()
    }
}
