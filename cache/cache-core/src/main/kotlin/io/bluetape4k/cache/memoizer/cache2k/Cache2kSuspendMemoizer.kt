package io.bluetape4k.cache.memoizer.cache2k

import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache2k를 사용하는 suspend memoizer를 생성합니다.
 *
 * ```kotlin
 * val memo = cache.suspendMemoizer<String, Int> { it.length }
 * // memo("abcd") == 4
 * ```
 */
fun <T: Any, R: Any> org.cache2k.Cache<T, R>.suspendMemoizer(
    evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> =
    Cache2kSuspendMemoizer(this, evaluator)

/**
 * suspend 함수를 Cache2k 기반 memoizer로 감쌉니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).withSuspendMemoizer(cache)
 * // memo("abc") == 3
 * ```
 */
fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemoizer(
    cache: org.cache2k.Cache<T, R>,
): SuspendMemoizer<T, R> =
    Cache2kSuspendMemoizer(cache, this)

/**
 * Cache2k 기반 [SuspendMemoizer] 구현체입니다.
 *
 * ```kotlin
 * val memo = SuspendCache2kMemoizer(cache) { key: String -> key.length }
 * // memo("abcd") == 4
 * ```
 */
/**
 * Cache2k 기반 [SuspendMemoizer] 구현체입니다.
 *
 * ## 재귀 안전성
 * 전역 Mutex를 evaluator 실행 중에 보유하면 재귀 memoizer(factorial, fibonacci)에서 데드락이 발생한다.
 * Kotlin Mutex는 재진입(reentrant)을 지원하지 않기 때문이다.
 * per-key Deferred 패턴은 lock 없이 evaluator를 실행하므로 재귀 호출이 안전하다.
 *
 * ```kotlin
 * val memo = SuspendCache2kMemoizer(cache) { key: String -> key.length }
 * // memo("abcd") == 4
 * ```
 */
class Cache2kSuspendMemoizer<in T: Any, out R: Any>(
    private val cache: org.cache2k.Cache<T, R>,
    private val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLoggingChannel()

    // per-key Deferred 맵: 같은 키에 대해 첫 번째 호출이 Deferred를 생성하고 이후 호출들이 await한다.
    private val inflightMap = ConcurrentHashMap<T, Deferred<R>>()
    private val clearMutex = Mutex()

    override suspend fun invoke(input: T): R {
        // 1단계: 빠른 경로 — 이미 캐시된 결과는 lock/Deferred 없이 즉시 반환
        cache.get(input)?.let { return it }

        // 2단계: per-key Deferred로 중복 evaluator 실행 방지.
        // computeIfAbsent는 atomic이므로 같은 키에 대해 Deferred가 하나만 생성된다.
        return coroutineScope {
            var createdByThisCall = false
            val deferred = inflightMap.computeIfAbsent(input) {
                createdByThisCall = true
                async {
                    evaluator(input)
                }
            }
            try {
                val result = deferred.await()
                this@Cache2kSuspendMemoizer.cache.put(input, result)
                result
            } finally {
                // evaluator 실패 후에도 in-flight 항목을 정리해 다음 호출이 재시도할 수 있게 한다.
                if (createdByThisCall || deferred.isCompleted) {
                    inflightMap.remove(input, deferred)
                }
            }
        }
    }

    override suspend fun clear() {
        clearMutex.withLock {
            inflightMap.clear()
            cache.clear()
        }
    }
}
