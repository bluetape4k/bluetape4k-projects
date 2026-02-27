package io.bluetape4k.cache.memorizer.jcache

import io.bluetape4k.cache.jcache.getOrPut
import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun <T: Any, R: Any> javax.cache.Cache<T, R>.suspendMemorizer(
    evaluator: suspend (T) -> R,
): SuspendJCacheMemorizer<T, R> =
    SuspendJCacheMemorizer(this, evaluator)

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
