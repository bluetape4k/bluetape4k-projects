package io.bluetape4k.cache.memoizer.jcache

import io.bluetape4k.cache.jcache.getOrPut
import io.bluetape4k.cache.memoizer.Memoizer
import io.bluetape4k.logging.KLogging
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import kotlin.concurrent.withLock

/**
 * JCache를 이용하여 [JCacheMemoizer]를 생성합니다.
 */
fun <T : Any, R : Any> Cache<T, R>.memoizer(evaluator: (T) -> R): JCacheMemoizer<T, R> = JCacheMemoizer(this, evaluator)

/**
 * 함수를 JCache 기반 [JCacheMemoizer]로 감쌉니다.
 */
fun <T : Any, R : Any> ((T) -> R).withMemoizer(cache: javax.cache.Cache<T, R>): JCacheMemoizer<T, R> =
    JCacheMemoizer(cache, this)

/**
 * JCache를 이용하여 메소드의 실행 결과를 기억하여, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 */
class JCacheMemoizer<in T : Any, out R : Any>(
    private val jcache: javax.cache.Cache<T, R>,
    private val evaluator: (T) -> R,
) : Memoizer<T, R> {
    companion object : KLogging()

    private val lock = ReentrantLock()

    override fun invoke(input: T): R = jcache.getOrPut(input) { evaluator(input) }

    override fun clear() {
        lock.withLock {
            jcache.clear()
        }
    }
}
