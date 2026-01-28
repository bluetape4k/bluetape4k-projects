package io.bluetape4k.utils

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Singleton 객체를 보관해주는 객체입니다.
 *
 * ```
 * class Manager private constructor(private val context:Context) {
 *     companion object: SingletonHolder<Manager> { Manager(context) }
 *     fun doSutff() {}
 * }
 *
 * // Use singleton
 * val manager = Manager.getInstance()
 * manager.doStuff()
 * ````
 */
open class SingletonHolder<T: Any>(factory: () -> T) {

    @Volatile
    private var _factory: (() -> T)? = factory
    private val instance = atomic<T?>(null)
    private val lock = ReentrantLock()

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
