package io.bluetape4k.cache.memorizer.jcache

import io.bluetape4k.cache.jcache.getOrPut
import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Deprecated(
    message = "suspendMemorizer()는 suspendMemoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("suspendMemoizer(evaluator)", "io.bluetape4k.cache.memoizer.jcache.suspendMemoizer"),
    level = DeprecationLevel.WARNING
)
fun <T: Any, R: Any> javax.cache.Cache<T, R>.suspendMemorizer(
    evaluator: suspend (T) -> R,
): SuspendJCacheMemorizer<T, R> =
    SuspendJCacheMemorizer(this, evaluator)

@Deprecated(
    message = "withSuspendMemorizer()는 withSuspendMemoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("withSuspendMemoizer(jcache)", "io.bluetape4k.cache.memoizer.jcache.withSuspendMemoizer"),
    level = DeprecationLevel.WARNING
)
fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemorizer(
    jcache: javax.cache.Cache<T, R>,
): SuspendJCacheMemorizer<T, R> =
    SuspendJCacheMemorizer(jcache, this)

/**
 * [javax.cache.Cache]를 저장소로 사용하는 [SuspendMemorizer] 구현체입니다.
 *
 * @param jcache [javax.cache.Cache] 인스턴스
 * @param evaluator 계산 함수
 */
@Deprecated(
    message = "SuspendJCacheMemorizer는 SuspendJCacheMemoizer로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("SuspendJCacheMemoizer", "io.bluetape4k.cache.memoizer.jcache.SuspendJCacheMemoizer"),
    level = DeprecationLevel.WARNING
)
class SuspendJCacheMemorizer<T: Any, R: Any>(
    private val jcache: javax.cache.Cache<T, R>,
    private val evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> {

    companion object: KLoggingChannel()

    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return jcache.getOrPut(input) { evaluator(input) }
    }

    override suspend fun clear() {
        mutex.withLock {
            jcache.clear()
        }
    }
}
