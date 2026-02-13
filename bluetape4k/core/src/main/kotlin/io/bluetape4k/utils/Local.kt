package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Local.save
import java.io.Serializable
import java.util.*

/**
 * Thread context 별로 Local storage를 제공하는 object 입니다.
 *
 * ```
 * Local["key"] = "42"        // set
 * val value = Local["key"]   // 42
 * ```
 *
 * @see ThreadLocal
 */
@Suppress("UNCHECKED_CAST")
object Local: KLogging() {

    private val threadLocal: ThreadLocal<java.util.HashMap<Any, Any?>> by lazy {
        object: ThreadLocal<java.util.HashMap<Any, Any?>>() {
            override fun initialValue(): java.util.HashMap<Any, Any?> {
                return HashMap()
            }
        }
    }

    private val storage: java.util.HashMap<Any, Any?> get() = threadLocal.get()

    /**
     * 현재 스레드의 로컬 저장소 스냅샷을 반환합니다.
     */
    fun save(): java.util.HashMap<Any, Any?> = storage.clone() as java.util.HashMap<Any, Any?>

    /**
     * [save]로 보관한 저장소를 현재 스레드에 복원합니다.
     */
    fun restore(saved: java.util.HashMap<Any, Any?>) {
        threadLocal.set(saved)
    }

    @JvmName("getObject")
    operator fun get(key: Any): Any? = storage[key]

    operator fun <T: Any> get(key: Any): T? = storage[key] as? T

    operator fun <T: Any> set(key: Any, value: T?) {
        when (value) {
            null -> storage.remove(key)
            else -> storage[key] = value
        }
    }

    fun clearAll() {
        log.debug { "Clear local storage." }
        storage.clear()
    }

    /**
     * 키가 없으면 [defaultValue] 결과를 저장하고 반환합니다.
     */
    fun <T: Any> getOrPut(key: Any, defaultValue: () -> T?): T? {
        return storage.getOrPut(key, defaultValue) as? T
    }

    /**
     * 키를 제거하고 기존 값을 반환합니다.
     */
    fun <T: Any> remove(key: Any): T? {
        return storage.remove(key) as? T
    }
}

/**
 * [Local]을 사용해 타입별 키를 숨긴 스토리지를 제공합니다.
 */
internal class LocalStorage<T: Any>: Serializable {

    private val key: UUID = UUID.randomUUID()

    fun get(): T? = Local.get<T>(key)

    fun set(value: T?) {
        Local[key] = value
    }

    fun update(value: T?) {
        set(value)
    }

    fun clear(): T? {
        return Local.remove(key)
    }
}
