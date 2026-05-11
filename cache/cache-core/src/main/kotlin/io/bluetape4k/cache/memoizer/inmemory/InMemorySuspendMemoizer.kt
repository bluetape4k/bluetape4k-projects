package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

/**
 * InMemory를 이용하여 [InMemorySuspendMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val memo = (suspend { key: String -> key.length }).suspendMemoizer()
 * val result = memo("hello")
 * // result == 5
 * val result2 = memo("hello")  // 캐시에서 즉시 반환
 * // result2 == 5
 * ```
 */
fun <T: Any, R: Any> (suspend (T) -> R).suspendMemoizer(): InMemorySuspendMemoizer<T, R> =
    InMemorySuspendMemoizer(this)

/**
 * 로컬 메모리에 suspend evaluator 실행 결과를 저장합니다.
 *
 * ## 동시성 안전성
 * `ConcurrentHashMap.getOrPut` (Kotlin 확장)은 동시에 여러 코루틴이 같은 키로 호출할 경우
 * lambda를 여러 번 실행할 수 있다. `computeIfAbsent`는 suspend 람다를 지원하지 않으므로
 * per-key [Deferred] 패턴을 사용한다.
 *
 * ## 재귀 안전성
 * 전역 Mutex를 evaluator 실행 중에 보유하면 재귀 memoizer(factorial, fibonacci)에서 데드락이 발생한다.
 * Kotlin Mutex는 재진입(reentrant)을 지원하지 않기 때문이다.
 * per-key Deferred 패턴은 lock을 보유하지 않고 evaluator를 실행하므로 재귀 호출이 안전하다.
 *
 * ```kotlin
 * val memo = InMemorySuspendMemoizer<String, Int> { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 */
class InMemorySuspendMemoizer<in T: Any, out R: Any>(
    private val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val resultCache = ConcurrentHashMap<T, R>()

    // per-key Deferred 맵: 같은 키에 대해 첫 번째 호출이 Deferred를 생성하고 이후 호출들이 await한다.
    // ConcurrentHashMap.computeIfAbsent는 논블로킹이므로 Deferred 생성에만 사용 가능하다.
    private val inflightMap = ConcurrentHashMap<T, Deferred<R>>()

    override suspend fun invoke(input: T): R {
        // 1단계: 빠른 경로 — 이미 캐시된 결과는 lock/Deferred 없이 즉시 반환
        resultCache[input]?.let { return it }

        // 2단계: per-key Deferred로 중복 evaluator 실행 방지.
        // computeIfAbsent는 atomic이므로 같은 키에 대해 Deferred가 하나만 생성된다.
        // evaluator는 Deferred 내부에서 실행되므로 lock을 보유하지 않아 재귀 호출이 안전하다.
        return coroutineScope {
            var createdByThisCall = false
            val deferred = inflightMap.computeIfAbsent(input) {
                createdByThisCall = true
                async {
                    log.trace { "Cache miss for key: $input, evaluating..." }
                    evaluator(input)
                }
            }
            try {
                val result = deferred.await()
                resultCache[input] = result
                result
            } finally {
                // 실패/취소된 Deferred가 남으면 이후 같은 키 호출이 영구적으로 같은 실패를 재사용한다.
                // 다만 다른 waiter가 취소된 것만으로 진행 중인 계산을 제거하지 않도록 생성자 또는 완료 상태에서만 제거한다.
                if (createdByThisCall || deferred.isCompleted) {
                    inflightMap.remove(input, deferred)
                }
            }
        }
    }

    override suspend fun clear() {
        inflightMap.clear()
        resultCache.clear()
        log.trace { "Cleared in-memory cache." }
    }
}
