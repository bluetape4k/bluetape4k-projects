package io.bluetape4k.utils

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Singleton 객체를 스레드 안전하게 보관해주는 클래스입니다.
 *
 * ```kotlin
 * class DatabasePool private constructor(val url: String) {
 *     companion object : SingletonHolder<DatabasePool>({ DatabasePool("jdbc:h2:mem:test") })
 *
 *     fun query(sql: String): List<String> = listOf()
 * }
 *
 * // 어디서나 동일 인스턴스 반환
 * val pool1 = DatabasePool.getInstance()
 * val pool2 = DatabasePool.getInstance()
 * // pool1 === pool2  (동일 인스턴스)
 * pool1.query("SELECT 1")
 * ```
 */
open class SingletonHolder<T: Any>(factory: () -> T) {

    @Volatile
    private var _factory: (() -> T)? = factory
    private val instance = atomic<T?>(null)
    private val lock = ReentrantLock()

    /**
     * 싱글톤 인스턴스를 반환합니다. 최초 호출 시 factory 람다로 생성하고, 이후에는 캐시된 인스턴스를 반환합니다.
     *
     * ```kotlin
     * val instance1 = holder.getInstance()
     * val instance2 = holder.getInstance()
     * // instance1 === instance2
     * ```
     *
     * @return 싱글톤 인스턴스
     */
    fun getInstance(): T {
        instance.value?.let { return it }

        lock.withLock {
            instance.value?.let { return it }

            val created = _factory?.invoke()
            instance.compareAndSet(null, created)
            _factory = null
            return created!!
        }
    }
}
