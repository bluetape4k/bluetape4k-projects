package io.bluetape4k.cache.memorizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * Caffeine [Cache]를 사용하는 suspend memorizer를 생성합니다.
 *
 * ## 동작/계약
 * - 입력 키가 캐시에 있으면 evaluator를 호출하지 않고 캐시 값을 반환합니다.
 * - 캐시에 없으면 [evaluator] 실행 결과를 캐시에 저장한 뒤 반환합니다.
 * - 캐시 저장/조회는 Caffeine 동기 API를 사용합니다.
 *
 * ```kotlin
 * val memo = cache.suspendMemorizer<String, Int> { it.length }
 * val size = memo("abcd")
 * // size == 4
 * ```
 */
fun <T: Any, R: Any> Cache<T, R>.suspendMemorizer(
    @BuilderInference evaluator: suspend (T) -> R,
): SuspendCaffeineMemorizer<T, R> =
    SuspendCaffeineMemorizer(this, evaluator)

/**
 * suspend 함수를 Caffeine 캐시와 결합한 memorizer로 감쌉니다.
 *
 * ## 동작/계약
 * - [cache]를 저장소로 사용해 동일 입력의 결과를 재사용합니다.
 * - 캐시에 값이 없을 때만 원본 suspend 함수를 실행합니다.
 * - 새 [SuspendCaffeineMemorizer] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).withSuspendMemorizer(cache)
 * // memo("abc") == 3
 * ```
 */
fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemorizer(cache: Cache<T, R>): SuspendCaffeineMemorizer<T, R> =
    SuspendCaffeineMemorizer(cache, this)

/**
 * Caffeine 기반 [SuspendMemorizer] 구현체입니다.
 *
 * ## 동작/계약
 * - [invoke]는 캐시 조회 후 miss 시 evaluator를 실행해 값을 저장합니다.
 * - [clear]는 mutex로 직렬화된 상태에서 `cache.cleanUp()`을 호출합니다.
 * - 입력 타입 `T`는 null 비허용이며 캐시 키로 그대로 사용됩니다.
 *
 * ```kotlin
 * val memo = SuspendCaffeineMemorizer(cache) { key: String -> key.length }
 * // memo("abcd") == 4
 * ```
 */
class SuspendCaffeineMemorizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    @BuilderInference private val evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> {

    companion object: KLoggingChannel()

    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return cache.getIfPresent(input)
            ?: run {
                val result = evaluator(input)
                cache.put(input, result)
                result
            }
    }

    override suspend fun clear() {
        mutex.withLock {
            cache.cleanUp()
        }
    }
}
