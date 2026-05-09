package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Caffeine [Cache]를 사용하는 suspend memoizer를 생성합니다.
 *
 * ```kotlin
 * val memo = cache.suspendMemoizer<String, Int> { it.length }
 * val size = memo("abcd")
 * // size == 4
 * ```
 */
fun <T: Any, R: Any> Cache<T, R>.suspendMemoizer(
    evaluator: suspend (T) -> R,
): CaffeineSuspendMemoizer<T, R> =
    CaffeineSuspendMemoizer(this, evaluator)

/**
 * suspend 함수를 Caffeine 캐시와 결합한 memoizer로 감쌉니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).withSuspendMemoizer(cache)
 * // memo("abc") == 3
 * ```
 */
fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemoizer(cache: Cache<T, R>): CaffeineSuspendMemoizer<T, R> =
    CaffeineSuspendMemoizer(cache, this)

/**
 * Caffeine 기반 [SuspendMemoizer] 구현체입니다.
 *
 * ## 동시성 안전성
 * 단순 getIfPresent + put 패턴은 동시 호출 시 동일 키에 대해 evaluator가 여러 번 실행될 수 있다.
 * `Caffeine.computeIfAbsent`는 suspend 람다를 지원하지 않으므로 per-key [Deferred] 패턴을 사용한다.
 *
 * ## 재귀 안전성
 * 전역 Mutex를 evaluator 실행 중에 보유하면 재귀 memoizer(factorial, fibonacci)에서 데드락이 발생한다.
 * Kotlin Mutex는 재진입(reentrant)을 지원하지 않기 때문이다.
 * per-key Deferred 패턴은 lock 없이 evaluator를 실행하므로 재귀 호출이 안전하다.
 *
 * ```kotlin
 * val memo = SuspendCaffeineMemoizer(cache) { key: String -> key.length }
 * // memo("abcd") == 4
 * ```
 */
class CaffeineSuspendMemoizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    private val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLoggingChannel()

    // per-key Deferred 맵: 같은 키에 대해 첫 번째 호출이 Deferred를 생성하고 이후 호출들이 await한다.
    // Kotlin Mutex는 재진입을 지원하지 않아 재귀 evaluator에서 데드락이 발생하므로 이 방식을 택한다.
    private val inflightMap = ConcurrentHashMap<T, Deferred<R>>()
    private val clearMutex = Mutex()

    override suspend fun invoke(input: T): R {
        // 1단계: 빠른 경로 — 이미 캐시된 결과는 lock/Deferred 없이 즉시 반환
        cache.getIfPresent(input)?.let { return it }

        // 2단계: per-key Deferred로 중복 evaluator 실행 방지.
        // computeIfAbsent는 atomic이므로 같은 키에 대해 Deferred가 하나만 생성된다.
        // evaluator는 Deferred 내부에서 실행되므로 lock을 보유하지 않아 재귀 호출이 안전하다.
        return coroutineScope {
            val deferred = inflightMap.computeIfAbsent(input) {
                async {
                    evaluator(input)
                }
            }
            val result = deferred.await()
            cache.put(input, result)
            inflightMap.remove(input, deferred)
            result
        }
    }

    override suspend fun clear() {
        clearMutex.withLock {
            inflightMap.clear()
            // cleanUp()은 만료된 항목만 제거하므로, 전체 초기화에는 invalidateAll()을 사용해야 한다.
            cache.invalidateAll()
        }
    }
}
